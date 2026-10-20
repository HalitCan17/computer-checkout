package com.swe307.pro2.controller;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.model.Computer;
import com.swe307.pro2.repository.ComputerRepository;
import com.swe307.pro2.service.ReservationService;

@RestController
public class ComputerController {

    private final ComputerRepository _repo;
    private final ReservationService _reservations;

    public ComputerController(ComputerRepository repo, ReservationService reservations) {
        _repo = repo;
        _reservations = reservations;
    }

    @GetMapping("/api/computers")
    public List<Computer> all() {
        return _repo.findAll(Sort.by("id"));
    }

    @PostMapping("/api/computers")
    public ResponseEntity<?> create(@RequestBody Computer c) {
        ResponseEntity<?> error = check(c, null);
        if (error != null) {
            return error;
        }
        c.setId(null);
        return save(c, true);
    }

    @PutMapping("/api/computers/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Computer c) {
        Computer old = _repo.findById(id).orElse(null);
        if (old == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<?> error = check(c, id);
        if (error != null) {
            return error;
        }
        old.setBrand(c.getBrand());
        old.setModel(c.getModel());
        old.setSerialNumber(c.getSerialNumber());
        old.setAvailable(c.isAvailable());
        return save(old, false);
    }

    @DeleteMapping("/api/computers/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!_repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            _repo.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("Bu bilgisayarin rezervasyonu var, once rezervasyonlarini silin");
        }
        _reservations.clearView();
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> save(Computer c, boolean created) {
        try {
            Computer saved = _repo.save(c);
            _reservations.clearView();   // rezervasyon listesindeki marka/model eski kalmasin
            return created
                    ? ResponseEntity.created(URI.create("/api/computers/" + saved.getId())).body(saved)
                    : ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Kaydedilemedi: alan cok uzun veya gecersiz");
        }
    }

    private ResponseEntity<?> check(Computer c, Long selfId) {
        if (isBlank(c.getBrand()) || isBlank(c.getModel()) || isBlank(c.getSerialNumber())) {
            return ResponseEntity.badRequest().body("Marka, model ve seri numarasi zorunlu");
        }
        Computer same = _repo.findFirstBySerialNumber(c.getSerialNumber().trim()).orElse(null);
        if (same != null && !same.getId().equals(selfId)) {
            return ResponseEntity.status(409).body("Bu seri numarasi baska bir bilgisayarda var");
        }
        c.setSerialNumber(c.getSerialNumber().trim());
        return null;
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
