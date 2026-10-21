package com.swe307.pro2.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.swe307.pro2.model.Student;
import com.swe307.pro2.repository.StudentRepository;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository _repo;

    public StudentService(StudentRepository repo) {
        _repo = repo;
    }

    @Cacheable(value = "students", key = "'all'")
    public List<Student> findAll() {
        log.info("CACHE MISS: ogrenci listesi MySQL'den okundu");
        return _repo.findAll(Sort.by("id"));
    }

    @Cacheable(value = "student", key = "#id", unless = "#result == null")
    public Optional<Student> findById(Long id) {
        log.info("CACHE MISS: ogrenci {} MySQL'den okundu", id);
        return _repo.findById(id);
    }

    // Ekleme ve guncelleme: once MySQL, sonra Redis.
    // Ogrenci bilgisi rezervasyon listesinde de gorundugu icin rezervasyon cache'i de silinir.
    @Caching(
            put = @CachePut(value = "student", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "students", allEntries = true),
                    @CacheEvict(value = { "reservations", "reservation" }, allEntries = true) })
    public Student save(Student s) {
        return _repo.save(s);
    }

    @Caching(evict = {
            @CacheEvict(value = "student", key = "#id"),
            @CacheEvict(value = "students", allEntries = true),
            @CacheEvict(value = { "reservations", "reservation" }, allEntries = true) })
    public void delete(Long id) {
        _repo.deleteById(id);
    }
}
