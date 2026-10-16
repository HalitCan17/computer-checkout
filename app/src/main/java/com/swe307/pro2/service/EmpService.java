package com.swe307.pro2.service;

import org.springframework.stereotype.Service;
import java.util.List;
import com.swe307.pro2.dto.EmpDto;

import com.swe307.pro2.repository.EmpRepository;

@Service 
public class EmpService {
    private final EmpRepository _repo;

    public EmpService(EmpRepository repo){
        _repo=repo;
    }

    public List<EmpDto> findAll(){
        return _repo.findAllView();
    }
    
}
