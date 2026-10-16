package com.swe307.pro2.dto;

import java.math.BigDecimal;

public record EmpDto(
    Integer empno,
    String ename,
    String managerName,
    BigDecimal sal,
    BigDecimal comm,
    String dname,
    String img
)
{
    
}

    
