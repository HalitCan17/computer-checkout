package com.swe307.pro2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.swe307.pro2.service.EmpService;

@Controller
public class PageController {

    private final EmpService _service;

    public PageController(EmpService service) {
        _service = service;
    }

    @GetMapping("/")
    public String index (Model model) {
        model.addAttribute("emps", _service.findAll());
        return "emps";
    }

}
