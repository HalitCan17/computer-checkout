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

import com.swe307.pro2.model.Computer;
import com.swe307.pro2.repository.ComputerRepository;

@Service
public class ComputerService {

    private static final Logger log = LoggerFactory.getLogger(ComputerService.class);

    private final ComputerRepository _repo;

    public ComputerService(ComputerRepository repo) {
        _repo = repo;
    }

    @Cacheable(value = "computers", key = "'all'")
    public List<Computer> findAll() {
        log.info("CACHE MISS: bilgisayar listesi MySQL'den okundu");
        return _repo.findAll(Sort.by("id"));
    }

    @Cacheable(value = "computer", key = "#id", unless = "#result == null")
    public Optional<Computer> findById(Long id) {
        log.info("CACHE MISS: bilgisayar {} MySQL'den okundu", id);
        return _repo.findById(id);
    }

    // Ekleme ve guncelleme: once MySQL, sonra Redis.
    // Bilgisayar bilgisi rezervasyon listesinde de gorundugu icin rezervasyon cache'i de silinir.
    @Caching(
            put = @CachePut(value = "computer", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "computers", allEntries = true),
                    @CacheEvict(value = { "reservations", "reservation" }, allEntries = true) })
    public Computer save(Computer c) {
        return _repo.save(c);
    }

    @Caching(evict = {
            @CacheEvict(value = "computer", key = "#id"),
            @CacheEvict(value = "computers", allEntries = true),
            @CacheEvict(value = { "reservations", "reservation" }, allEntries = true) })
    public void delete(Long id) {
        _repo.deleteById(id);
    }
}
