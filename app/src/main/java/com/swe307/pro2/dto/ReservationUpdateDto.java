package com.swe307.pro2.dto;

import java.time.LocalDateTime;

public record ReservationUpdateDto(LocalDateTime reservationDate, LocalDateTime returnDate) {
}