package com.swe307.pro2.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.ReservationCreateDto;
import com.swe307.pro2.dto.ReservationDto;
import com.swe307.pro2.dto.ReservationUpdateDto;
import com.swe307.pro2.model.Computer;
import com.swe307.pro2.model.Reservation;
import com.swe307.pro2.model.Student;
import com.swe307.pro2.repository.ComputerRepository;
import com.swe307.pro2.repository.ReservationRepository;
import com.swe307.pro2.repository.StudentRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private static final String VIEW_KEY = "reservation:view";
    private static final String PENDING_KEY = "reservation:pending";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final ReservationRepository _repo;
    private final StudentRepository _students;
    private final ComputerRepository _computers;
    private final StringRedisTemplate _redis;
    private final JsonMapper _json;

    public ReservationService(ReservationRepository repo, StudentRepository students,
                              ComputerRepository computers, StringRedisTemplate redis, JsonMapper json) {
        _repo = repo;
        _students = students;
        _computers = computers;
        _redis = redis;
        _json = json;
    }

    // ---------- READ: Redis'ten, bossa MySQL'den (JOIN) doldurur ----------
    public List<ReservationDto> findAll() {
        if (Boolean.TRUE.equals(_redis.hasKey(VIEW_KEY))) {
            log.info("CACHE HIT: rezervasyon listesi Redis'ten donuldu");
            return _redis.opsForHash().values(VIEW_KEY).stream()
                    .map(v -> _json.readValue((String) v, ReservationDto.class))
                    .sorted(Comparator.comparing(ReservationDto::reservationDate)
                            .thenComparing(ReservationDto::id))
                    .toList();
        }
        return loadFromDatabase();
    }

    private List<ReservationDto> loadFromDatabase() {
        flushPending(); // bekleyen guncellemeler MySQL'e gitmeden vitrin eski veriyle dolmasin
        log.info("CACHE MISS: Redis bos, MySQL'e gidildi (JOIN)");
        List<ReservationDto> list = _repo.findAllView();
        if (list.isEmpty()) {
            return list;
        }
        Map<String, String> map = new HashMap<>();
        for (ReservationDto r : list) {
            map.put(String.valueOf(r.id()), _json.writeValueAsString(r));
        }
        _redis.opsForHash().putAll(VIEW_KEY, map);
        _redis.expire(VIEW_KEY, TTL);
        return list;
    }

    // ---------- CREATE: once MySQL, sonra Redis ----------
    public ReservationDto create(ReservationCreateDto d) {
        validate(d);

        Student student = _students.findFirstByStudentNumber(d.studentNumber().trim())
                .orElseThrow(() -> new IllegalArgumentException("Boyle bir ogrenci numarasi yok: " + d.studentNumber()));
        Computer computer = _computers.findById(d.computerId())
                .orElseThrow(() -> new IllegalArgumentException("Boyle bir bilgisayar yok: " + d.computerId()));

        if (!computer.isAvailable()) {
            throw new IllegalStateException("Bu bilgisayar su an musait degil");
        }
        flushPending(); // MySQL'deki cakisma kontrolu bekleyen guncellemeleri de gorsun
        if (_repo.existsOverlap(computer.getId(), d.reservationDate(), d.returnDate())) {
            throw new IllegalStateException("Bu bilgisayar secilen saatlerde baska bir rezervasyonda");
        }

        Reservation r = new Reservation();
        r.setReservationDate(d.reservationDate());
        r.setReturnDate(d.returnDate());
        r.setStudentId(student.getId());
        r.setComputerId(computer.getId());
        _repo.save(r);                                                  // 1) once MySQL
        log.info("CREATE: rezervasyon {} MySQL'e yazildi", r.getId());

        ReservationDto view = new ReservationDto(r.getId(), student.getId(), student.getFirstName(),
                student.getLastName(), student.getStudentNumber(), student.getDepartment(), student.getPhoto(),
                computer.getId(), computer.getBrand(), computer.getModel(), computer.getSerialNumber(),
                r.getReservationDate(), r.getReturnDate());
        try {
            // vitrin hic yoksa YAZMA: tek kayitlik yarim vitrin olusur; sonraki okuma MySQL'den komple yukler
            if (Boolean.TRUE.equals(_redis.hasKey(VIEW_KEY))) {
                _redis.opsForHash().put(VIEW_KEY, String.valueOf(r.getId()), _json.writeValueAsString(view)); // 2) sonra Redis
            }
        } catch (Exception ex) {
            invalidateView(ex);
        }
        return view;
    }

    // ---------- DELETE: once MySQL, sonra Redis ----------
    public boolean delete(Long id) {
        if (!_repo.existsById(id)) {
            return false;
        }
        _repo.deleteById(id);                                           // 1) once MySQL
        log.info("DELETE: rezervasyon {} MySQL'den silindi", id);
        try {
            _redis.opsForHash().delete(VIEW_KEY, String.valueOf(id));   // 2) sonra Redis
            _redis.opsForHash().delete(PENDING_KEY, String.valueOf(id)); // bekleyen guncellemesi varsa o da bosa gider
        } catch (Exception ex) {
            invalidateView(ex);
        }
        return true;
    }

    // ---------- UPDATE (write-behind): once Redis, MySQL'e sonra (flushPending) ----------
    public Optional<ReservationDto> update(Long id, ReservationUpdateDto d) {
        if (d == null || d.reservationDate() == null || d.returnDate() == null) {
            throw new IllegalArgumentException("Rezervasyon ve iade tarihi zorunlu");
        }
        if (!d.returnDate().isAfter(d.reservationDate())) {
            throw new IllegalArgumentException("Iade tarihi rezervasyon tarihinden sonra olmali");
        }

        List<ReservationDto> all = findAll();               // vitrin bossa MySQL'den doldurur
        ReservationDto old = all.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
        if (old == null) {
            return Optional.empty();
        }
        // cakisma kontrolu vitrinde (en guncel veri orada): ayni bilgisayar, baska rezervasyonlar
        boolean overlap = all.stream().anyMatch(r -> !r.id().equals(id)
                && r.computerId().equals(old.computerId())
                && r.reservationDate().isBefore(d.returnDate())
                && r.returnDate().isAfter(d.reservationDate()));
        if (overlap) {
            throw new IllegalStateException("Bu bilgisayar secilen saatlerde baska bir rezervasyonda");
        }

        ReservationDto updated = new ReservationDto(old.id(), old.studentId(), old.firstName(), old.lastName(),
                old.studentNumber(), old.department(), old.photo(), old.computerId(), old.brand(), old.model(),
                old.serialNumber(), d.reservationDate(), d.returnDate());
        _redis.opsForHash().put(VIEW_KEY, String.valueOf(id), _json.writeValueAsString(updated));  // 1) Redis (vitrin)
        _redis.opsForHash().put(PENDING_KEY, String.valueOf(id), _json.writeValueAsString(d));     // 2) MySQL'e gidecekler listesi
        log.info("UPDATE: rezervasyon {} Redis'e yazildi, MySQL'e sonra aktarilacak", id);
        return Optional.of(updated);
    }

    // Periyodik: bekleyen guncellemeleri MySQL'e yazar. Hem zamanlayici hem okuma/ekleme cagirir.
    @Scheduled(fixedDelayString = "${reservation.flush-ms:30000}")
    public synchronized void flushPending() {
        try {
            Map<Object, Object> pending = _redis.opsForHash().entries(PENDING_KEY);
            for (Map.Entry<Object, Object> e : pending.entrySet()) {
                Long id = Long.valueOf((String) e.getKey());
                ReservationUpdateDto d = _json.readValue((String) e.getValue(), ReservationUpdateDto.class);
                _repo.findById(id).ifPresent(r -> {
                    r.setReservationDate(d.reservationDate());
                    r.setReturnDate(d.returnDate());
                    _repo.save(r);
                    log.info("FLUSH: rezervasyon {} MySQL'e aktarildi", id);
                });
                // bu arada ayni kayit tekrar guncellendiyse dokunma, bir sonraki turda gider
                Object current = _redis.opsForHash().get(PENDING_KEY, e.getKey());
                if (e.getValue().equals(current)) {
                    _redis.opsForHash().delete(PENDING_KEY, e.getKey());
                }
            }
        } catch (Exception ex) {
            log.warn("FLUSH basarisiz, bekleyenler Redis'te duruyor, sonraki turda denenecek: {}", ex.toString());
        }
    }

        // ---------- Ogrenci fotografi degisti: vitrindeki o ogrencinin tum satirlarini guncelle ----------
    public void refreshStudentPhoto(Long studentId, String photo) {
        try {
            if (!Boolean.TRUE.equals(_redis.hasKey(VIEW_KEY))) {
                return;                                     // vitrin yok: sonraki okuma MySQL'den taze yukler
            }
            Map<String, String> changed = new HashMap<>();
            for (Map.Entry<Object, Object> e : _redis.opsForHash().entries(VIEW_KEY).entrySet()) {
                ReservationDto r = _json.readValue((String) e.getValue(), ReservationDto.class);
                if (r.studentId().equals(studentId)) {
                    ReservationDto n = new ReservationDto(r.id(), r.studentId(), r.firstName(), r.lastName(),
                            r.studentNumber(), r.department(), photo, r.computerId(), r.brand(), r.model(),
                            r.serialNumber(), r.reservationDate(), r.returnDate());
                    changed.put((String) e.getKey(), _json.writeValueAsString(n));
                }
            }
            if (!changed.isEmpty()) {
                _redis.opsForHash().putAll(VIEW_KEY, changed);
            }
        } catch (Exception ex) {
            invalidateView(ex);
        }
    }

    // Ogrenci/bilgisayar bilgisi degisti: vitrini sil, sonraki okuma MySQL'den taze yuklesin
    public void clearView() {
        try {
            _redis.delete(VIEW_KEY);
        } catch (Exception ignore) {
            // Redis kapali: vitrin TTL ile kendiliginden bitecek
        }
    }

    // MySQL yazildi ama Redis yazilamadiysa: vitrini sil, sonraki okuma MySQL'den taze yuklesin
    private void invalidateView(Exception cause) {
        log.warn("Redis vitrini guncellenemedi, onbellek temizleniyor: {}", cause.toString());
        try {
            _redis.delete(VIEW_KEY);
        } catch (Exception ignore) {
            // Redis tamamen kapali: vitrin zaten TTL ile kendiliginden bitecek
        }
    }

    private void validate(ReservationCreateDto d) {
        if (d == null || d.studentNumber() == null || d.studentNumber().isBlank()) {
            throw new IllegalArgumentException("Ogrenci numarasi zorunlu");
        }
        if (d.computerId() == null) {
            throw new IllegalArgumentException("Bilgisayar secilmeli");
        }
        if (d.reservationDate() == null || d.returnDate() == null) {
            throw new IllegalArgumentException("Rezervasyon ve iade tarihi zorunlu");
        }
        if (!d.returnDate().isAfter(d.reservationDate())) {
            throw new IllegalArgumentException("Iade tarihi rezervasyon tarihinden sonra olmali");
        }
    }
}