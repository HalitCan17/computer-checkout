package com.swe307.pro2.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.ReservationCreateDto;
import com.swe307.pro2.dto.ReservationDto;
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
        } catch (Exception ex) {
            invalidateView(ex);
        }
        return true;
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