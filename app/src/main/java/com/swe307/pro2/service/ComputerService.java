package com.swe307.pro2.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.ComputerDto;
import com.swe307.pro2.repository.ComputerRepository;

@Service 
public class ComputerService {

    private final ComputerRepository _repo;

    public ComputerService(ComputerRepository repo){
        _repo=repo;
    }

    public List<ComputerDto> findAll(){
        return _repo.findAll().stream()
        .map(c->new ComputerDto(c.getId(), c.getBrand(),c.getModel(), c.getSerialNumber(), c.isAvailable()))
        .sorted(Comparator.comparing(ComputerDto::id))
        .toList();
    }

    public List<ComputerDto> findAvailable(){
        return _repo.findByAvailableTrue().stream()
        .map(c->new ComputerDto(c.getId(), c.getBrand(),c.getModel(), c.getSerialNumber(), c.isAvailable()))
        .sorted(Comparator.comparing(ComputerDto::id))
        .toList();

    }
    
}
