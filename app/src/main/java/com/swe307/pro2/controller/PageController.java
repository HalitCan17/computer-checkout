package com.swe307.pro2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.swe307.pro2.service.ComputerService;
import com.swe307.pro2.service.ReservationService;

@Controller
public class PageController {

    private final ReservationService _reservations;
    private final ComputerService _computers;

    public PageController(ReservationService reservations, ComputerService computers) {
        _reservations = reservations;
        _computers = computers;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("reservations", _reservations.findAll());
        model.addAttribute("computers", _computers.findAll());
        return "reservations";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

}