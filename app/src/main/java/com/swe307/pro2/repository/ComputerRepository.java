package com.swe307.pro2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swe307.pro2.model.Computer;

public interface ComputerRepository extends JpaRepository<Computer, Long> {
    List<Computer> findByAvailableTrue();
}