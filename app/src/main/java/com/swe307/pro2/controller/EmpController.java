package com.swe307.pro2.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swe307.pro2.dto.EmpDto;
import com.swe307.pro2.service.EmpService;

@RestController 
public class EmpController {
    
    private final EmpService _service;

    public EmpController(EmpService service)
    {
        _service=service;
    }

    @GetMapping("/api/emps")
    public List<EmpDto> all()
    {
        return _service.findAll();
    }
    
}
