package com.swe307.pro2.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table (name="emp")
public class Emp {

    @Id 
    private Integer empno;
    private String ename;
    private String job;
    private Integer mgr;
    private LocalDate hiradate;
    private BigDecimal sal;
    private BigDecimal comm;
    private Integer deptno;
    private String img;

    
}
