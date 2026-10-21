package com.swe307.pro2.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.swe307.pro2.service.PhotoService;

@RestController
public class PhotoController {

    private final PhotoService _service;

    public PhotoController(PhotoService service) {
        _service = service;
    }

    @PostMapping("/api/students/{id}/photo")
    public ResponseEntity<?> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return _service.upload(id, file)
                    .<ResponseEntity<?>>map(name -> ResponseEntity.ok(Map.of("photo", name)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(503).body("Fotograf yuklenemedi (HDFS ulasilamiyor olabilir)");
        }
    }

    @DeleteMapping("/api/students/{id}/photo")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        return _service.remove(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
