package com.swe307.pro2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import com.swe307.pro2.service.ComputerService;
import com.swe307.pro2.service.ReservationService;
import com.swe307.pro2.service.StudentService;

@Controller
public class PageController {

    private final ReservationService _reservations;
    private final ComputerService _computers;
    private final StudentService _students;

    public PageController(ReservationService reservations, ComputerService computers, StudentService students) {
        _reservations = reservations;
        _computers = computers;
        _students = students;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("reservations", _reservations.findAll());
        model.addAttribute("computers", _computers.findAll());
        return "reservations";
    }

    @GetMapping("/students")
    public String students(Model model) {
        model.addAttribute("students", _students.findAll());
        return "students";
    }

    @GetMapping("/computers")
    public String computers(Model model) {
        model.addAttribute("computers", _computers.findAll());
        return "computers";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

}