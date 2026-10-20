package com.swe307.pro2.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.swe307.pro2.model.Student;
import com.swe307.pro2.repository.StudentRepository;

import tools.jackson.databind.json.JsonMapper;

@Component
public class StudentCache extends HashListCache<Student> {

    public StudentCache(StringRedisTemplate redis, JsonMapper json, StudentRepository repo) {
        super(redis, json, "student:view", Student.class, Student::getId, () -> repo.findAll(Sort.by("id")));
    }
}
