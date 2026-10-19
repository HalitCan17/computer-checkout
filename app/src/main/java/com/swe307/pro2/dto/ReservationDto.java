package com.swe307.pro2.dto;

import java.time.LocalDateTime;

public record ReservationDto(
    Long id,
    Long studentId,
    String firstName,
    String lastName,
    String studentNumber,
    String department,
    String photo,
    Long computerId,
    String brand,
    String model,
    String serialNumber,
    LocalDateTime reservationDate,
    LocalDateTime returnDate
) {
}