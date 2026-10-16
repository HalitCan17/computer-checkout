package com.swe307.pro2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="dept")
public class Dept{
    @Id
    private Integer deptno;
    private String dname;
    private String loc;
}
