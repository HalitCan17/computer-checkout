package com.swe307.pro2.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationCreateDto (String studentNumber,Long computerId,LocalDateTime reservationDate, LocalDateTime returnDate){

}
    

