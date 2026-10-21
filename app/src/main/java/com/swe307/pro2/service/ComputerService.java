package com.swe307.pro2.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.ComputerDto;
import com.swe307.pro2.model.Computer;

@Service
public class ComputerService {

    private final ComputerCache _cache;

    public ComputerService(ComputerCache cache) {
        _cache = cache;
    }

    public List<ComputerDto> findAll() {
        return _cache.findAll().stream().map(this::toDto).toList();
    }

    public List<ComputerDto> findAvailable() {
        return _cache.findAll().stream().filter(Computer::isAvailable).map(this::toDto).toList();
    }

    private ComputerDto toDto(Computer c) {
        return new ComputerDto(c.getId(), c.getBrand(), c.getModel(), c.getSerialNumber(), c.isAvailable());
    }
}
