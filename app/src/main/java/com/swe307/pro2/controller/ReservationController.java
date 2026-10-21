package com.swe307.pro2.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.dto.ReservationCreateDto;
import com.swe307.pro2.dto.ReservationDto;
import com.swe307.pro2.dto.ReservationUpdateDto;
import com.swe307.pro2.service.ReservationService;

@RestController
public class ReservationController {

    private final ReservationService _service;

    public ReservationController(ReservationService service) {
        _service = service;
    }

    @GetMapping("/api/reservations")
    public List<ReservationDto> all() {
        return _service.findAll();
    }

    @GetMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationDto> one(@PathVariable Long id) {
        return _service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<?> create(@RequestBody ReservationCreateDto dto) {
        try {
            ReservationDto created = _service.create(dto);
            return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

        @PutMapping("/api/reservations/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ReservationUpdateDto dto) {
        try {
            return _service.update(id, dto)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/reservations/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return _service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}