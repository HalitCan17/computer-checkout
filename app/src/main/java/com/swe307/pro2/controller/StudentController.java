package com.swe307.pro2.controller;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.model.Student;
import com.swe307.pro2.repository.StudentRepository;
import com.swe307.pro2.service.StudentService;

@RestController
public class StudentController {

    private final StudentRepository _repo;
    private final StudentService _service;

    public StudentController(StudentRepository repo, StudentService service) {
        _repo = repo;
        _service = service;
    }

    @GetMapping("/api/students")
    public List<Student> all() {
        return _service.findAll();
    }

    @GetMapping("/api/students/{id}")
    public ResponseEntity<Student> one(@PathVariable Long id) {
        return _service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/students")
    public ResponseEntity<?> create(@RequestBody Student s) {
        ResponseEntity<?> error = check(s, null);
        if (error != null) {
            return error;
        }
        s.setId(null);
        s.setPhoto(null);
        return save(s, true);
    }

    @PutMapping("/api/students/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Student s) {
        Student old = _repo.findById(id).orElse(null);
        if (old == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<?> error = check(s, id);
        if (error != null) {
            return error;
        }
        old.setFirstName(s.getFirstName());
        old.setLastName(s.getLastName());
        old.setDepartment(s.getDepartment());
        old.setStudentNumber(s.getStudentNumber());
        return save(old, false);
    }

    @DeleteMapping("/api/students/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!_repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            _service.delete(id);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("Bu ogrencinin rezervasyonu var, once rezervasyonlarini silin");
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> save(Student s, boolean created) {
        try {
            Student saved = _service.save(s);
            return created
                    ? ResponseEntity.created(URI.create("/api/students/" + saved.getId())).body(saved)
                    : ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Kaydedilemedi: alan cok uzun veya gecersiz");
        }
    }

    // null: sorun yok. Dolu: dondurulecek hata
    private ResponseEntity<?> check(Student s, Long selfId) {
        if (isBlank(s.getFirstName()) || isBlank(s.getLastName())
                || isBlank(s.getDepartment()) || isBlank(s.getStudentNumber())) {
            return ResponseEntity.badRequest().body("Ad, soyad, bolum ve ogrenci numarasi zorunlu");
        }
        Student same = _repo.findFirstByStudentNumber(s.getStudentNumber().trim()).orElse(null);
        if (same != null && !same.getId().equals(selfId)) {
            return ResponseEntity.status(409).body("Bu ogrenci numarasi baska bir ogrencide var");
        }
        s.setStudentNumber(s.getStudentNumber().trim());
        return null;
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
