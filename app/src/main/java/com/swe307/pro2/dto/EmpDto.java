package com.swe307.pro2.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpDto(
    Integer empno,
    String ename,
    String job,
    String managerName,
    LocalDate hireDate,
    BigDecimal sal,
    BigDecimal comm,
    String dname,
    String img
)
{
    
}

    
