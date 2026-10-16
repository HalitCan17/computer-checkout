package com.swe307.pro2.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.EmpDto;
import com.swe307.pro2.repository.EmpRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service 
public class EmpService {
    
    private static final Logger log =LoggerFactory.getLogger(EmpService.class);
    private static final String CACHE_KEY="emp:view:all";
    private static final Duration TTL= Duration.ofMinutes(5);
    
    
    private final EmpRepository _repo;
    private final StringRedisTemplate _redis;
    private final JsonMapper _json;

    public EmpService(EmpRepository repo,StringRedisTemplate redis, JsonMapper json){
        
        _repo=repo;
        _redis=redis;
        _json=json;
    }

    public List<EmpDto> findAll(){

        String cached =_redis.opsForValue().get(CACHE_KEY);
        if(cached!=null){
            log.info("CACHE HIT: liste Redisten Dönüldü");
            return _json.readValue(cached, new TypeReference<List<EmpDto>>(){});
        }

        log.info("CACHE MISS: Redis Boş, MySQL'e gidildi");
        List<EmpDto> list =_repo.findAllView();
        _redis.opsForValue().set(CACHE_KEY, _json.writeValueAsString(list), TTL);

        return list;
        
    } 
    
}
