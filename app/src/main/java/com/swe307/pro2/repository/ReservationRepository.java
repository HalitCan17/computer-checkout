package com.swe307.pro2.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swe307.pro2.dto.ReservationDto;
import com.swe307.pro2.model.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select new com.swe307.pro2.dto.ReservationDto(
            r.id, s.id, s.firstName, s.lastName, s.studentNumber, s.department, s.photo,
            c.id, c.brand, c.model, c.serialNumber, r.reservationDate, r.returnDate)
        from Reservation r
        join Student s on r.studentId = s.id
        join Computer c on r.computerId = c.id
        order by r.reservationDate, r.id
        """)
    List<ReservationDto> findAllView();

    @Query("""
        select count(r) > 0 from Reservation r
        where r.computerId = :computerId
          and r.reservationDate < :end
          and r.returnDate > :start
        """)
    boolean existsOverlap(@Param("computerId") Long computerId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    @Query("""
        select new com.swe307.pro2.dto.ReservationDto(
            r.id, s.id, s.firstName, s.lastName, s.studentNumber, s.department, s.photo,
            c.id, c.brand, c.model, c.serialNumber, r.reservationDate, r.returnDate)
        from Reservation r
        join Student s on r.studentId = s.id
        join Computer c on r.computerId = c.id
        where r.id = :id
        """)
    Optional<ReservationDto> findViewById(@Param("id") Long id);

    // Guncellemede: kaydin kendisi haric ayni bilgisayarda cakisan rezervasyon var mi?
    @Query("""
        select count(r) > 0 from Reservation r
        where r.id <> :id
          and r.computerId = :computerId
          and r.reservationDate < :end
          and r.returnDate > :start
        """)
    boolean existsOverlapExcept(@Param("id") Long id,
                                @Param("computerId") Long computerId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
