package com.swe307.pro2.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.swe307.pro2.model.Computer;
import com.swe307.pro2.repository.ComputerRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
public class ComputerCache extends HashListCache<Computer> {

    public ComputerCache(StringRedisTemplate redis, JsonMapper json, ComputerRepository repo) {
        super(redis, json, "computer:view", Computer.class, Computer::getId, () -> repo.findAll(Sort.by("id")));
    }
}
