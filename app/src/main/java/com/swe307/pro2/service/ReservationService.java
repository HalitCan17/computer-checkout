package com.swe307.pro2.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository _repo;
    private final StudentRepository _students;
    private final ComputerRepository _computers;

    public ReservationService(ReservationRepository repo, StudentRepository students, ComputerRepository computers) {
        _repo = repo;
        _students = students;
        _computers = computers;
    }

    // Liste Redis'te "reservations::all" anahtarinda. Redis'te varsa bu metot hic calismaz.
    @Cacheable(value = "reservations", key = "'all'")
    public List<ReservationDto> findAll() {
        log.info("CACHE MISS: rezervasyon listesi MySQL'den okundu");
        return _repo.findAllView();
    }

    // Tek kayit Redis'te "reservation::<id>" anahtarinda
    @Cacheable(value = "reservation", key = "#id", unless = "#result == null")
    public Optional<ReservationDto> findById(Long id) {
        log.info("CACHE MISS: rezervasyon {} MySQL'den okundu", id);
        return _repo.findViewById(id);
    }

    // Once MySQL'e yazilir, donen kayit Redis'e konur, liste Redis'ten silinir
    @Caching(
            put = @CachePut(value = "reservation", key = "#result.id"),
            evict = @CacheEvict(value = "reservations", allEntries = true))
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
        _repo.save(r);
        log.info("CREATE: rezervasyon {} MySQL'e yazildi", r.getId());

        return new ReservationDto(r.getId(), student.getId(), student.getFirstName(),
                student.getLastName(), student.getStudentNumber(), student.getDepartment(), student.getPhoto(),
                computer.getId(), computer.getBrand(), computer.getModel(), computer.getSerialNumber(),
                r.getReservationDate(), r.getReturnDate());
    }

    // Once MySQL'e yazilir, guncel kayit Redis'e konur, liste Redis'ten silinir
    @Caching(
            put = @CachePut(value = "reservation", key = "#id", unless = "#result == null"),
            evict = @CacheEvict(value = "reservations", allEntries = true))
    public Optional<ReservationDto> update(Long id, ReservationUpdateDto d) {
        if (d == null || d.reservationDate() == null || d.returnDate() == null) {
            throw new IllegalArgumentException("Rezervasyon ve iade tarihi zorunlu");
        }
        if (!d.returnDate().isAfter(d.reservationDate())) {
            throw new IllegalArgumentException("Iade tarihi rezervasyon tarihinden sonra olmali");
        }

        Reservation r = _repo.findById(id).orElse(null);
        if (r == null) {
            return Optional.empty();
        }
        if (_repo.existsOverlapExcept(id, r.getComputerId(), d.reservationDate(), d.returnDate())) {
            throw new IllegalStateException("Bu bilgisayar secilen saatlerde baska bir rezervasyonda");
        }

        r.setReservationDate(d.reservationDate());
        r.setReturnDate(d.returnDate());
        _repo.save(r);
        log.info("UPDATE: rezervasyon {} MySQL'e yazildi", id);
        return _repo.findViewById(id);
    }

    // Once MySQL'den silinir, sonra kayit ve liste Redis'ten silinir
    @Caching(evict = {
            @CacheEvict(value = "reservation", key = "#id"),
            @CacheEvict(value = "reservations", allEntries = true) })
    public boolean delete(Long id) {
        if (!_repo.existsById(id)) {
            return false;
        }
        _repo.deleteById(id);
        log.info("DELETE: rezervasyon {} MySQL'den silindi", id);
        return true;
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
