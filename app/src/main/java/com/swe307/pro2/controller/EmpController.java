package com.swe307.pro2.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.swe307.pro2.dto.EmpDto;
import com.swe307.pro2.dto.EmpUpdateDto;
import com.swe307.pro2.service.EmpService;
import com.swe307.pro2.service.PhotoService;

@RestController
public class EmpController {

    private static final Logger log = LoggerFactory.getLogger(EmpController.class);
    private final EmpService _service;
    private final PhotoService _photos;

    public EmpController(EmpService service,PhotoService photos) {
        _service = service;
        _photos=photos;
    }

    @GetMapping("/api/emps")
    public List<EmpDto> all() {
        return _service.findAll();
    }

    @PutMapping ("/api/emps/{empno}")
    public ResponseEntity<?> update(@PathVariable Integer empno, @RequestBody  EmpUpdateDto dto){
        try{
                return _service.update(empno ,dto)
                .map(ResponseEntity::ok)
                .orElseGet(()->ResponseEntity.notFound().build());
        
           }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
           }
    }
    @PostMapping("/api/emps/{empno}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Integer empno,
                                         @RequestParam("file") MultipartFile file) {
        try {
            return _photos.upload(empno, file)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Fotograf yuklenemedi (empno={})", empno, e);
            return ResponseEntity.status(503).body("Fotograf HDFS'e yazilamadi. HDFS calisiyor mu?");
        }
    }

}
