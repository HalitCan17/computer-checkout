package com.swe307.pro2.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swe307.pro2.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findFirstByStudentNumber(String studentNumber);
}