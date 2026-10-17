package com.swe307.pro2.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.dto.EmpDto;
import com.swe307.pro2.dto.EmpUpdateDto;
import com.swe307.pro2.service.EmpService;

@RestController
public class EmpController {

    private final EmpService _service;

    public EmpController(EmpService service) {
        _service = service;
    }

    @GetMapping("/api/emps")
    public List<EmpDto> all() {
        return _service.findAll();
    }

    @PutMapping ("/api/emps/{empno}")
    public ResponseEntity<?> Update(@PathVariable Integer empno, @RequestBody  EmpUpdateDto dto){
        try{
                return _service.update(empno ,dto)
                .map(ResponseEntity::ok)
                .orElseGet(()->ResponseEntity.notFound().build());
        
           }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
           }
    }

}
