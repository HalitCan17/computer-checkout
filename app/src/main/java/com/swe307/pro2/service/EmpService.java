package com.swe307.pro2.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.EmpDto;
import com.swe307.pro2.dto.EmpUpdateDto;
import com.swe307.pro2.repository.EmpRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class EmpService {

    private static final Logger log = LoggerFactory.getLogger(EmpService.class);
    private static final String VIEW_KEY = "emp:view";
    public static final String PENDING_KEY = "emp:pending";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final EmpRepository _repo;
    private final StringRedisTemplate _redis;
    private final JsonMapper _json;

    public EmpService(EmpRepository repo, StringRedisTemplate redis, JsonMapper json) {
        _repo = repo;
        _redis = redis;
        _json = json;
    }

    public List<EmpDto> findAll() {
        if (Boolean.TRUE.equals(_redis.hasKey(VIEW_KEY))) {
            log.info("CACHE HIT: liste Redis'ten donuldu");
            return _redis.opsForHash().values(VIEW_KEY).stream()
                    .map(v -> _json.readValue((String) v, EmpDto.class))
                    .sorted(Comparator.comparing(EmpDto::empno))
                    .toList();
        }
        return loadFromDatabase();
    }

    private List<EmpDto> loadFromDatabase() {
        log.info("CACHE MISS: Redis bos, MySQL'e gidildi");
        List<EmpDto> list = _repo.findAllView();
        if (list.isEmpty()) {
            return list;
        }
        Map<String, String> map = new HashMap<>();
        for (EmpDto e : list) {
            map.put(String.valueOf(e.empno()), _json.writeValueAsString(e));
        }
        _redis.opsForHash().putAll(VIEW_KEY, map);
        _redis.expire(VIEW_KEY, TTL);
        return list;
    }

    private Optional<EmpDto> findOne(Integer empno) {
        if (!Boolean.TRUE.equals(_redis.hasKey(VIEW_KEY))) {
            loadFromDatabase();
        }
        Object raw = _redis.opsForHash().get(VIEW_KEY, String.valueOf(empno));
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(_json.readValue((String) raw, EmpDto.class));
    }

    private void saveToRedis(EmpDto updated, EmpUpdateDto pending) {
        String field = String.valueOf(updated.empno());
        _redis.opsForHash().put(VIEW_KEY, field, _json.writeValueAsString(updated));
        _redis.opsForHash().put(PENDING_KEY, field, _json.writeValueAsString(pending));
        log.info("UPDATE: {} Redis'e yazildi, MySQL'e aktarim bekliyor", updated.empno());
    }

    public Optional<EmpDto> update(Integer empno, EmpUpdateDto dto) {
        if (dto == null || dto.ename() == null || dto.ename().isBlank()) {
            throw new IllegalArgumentException("Ad bos olamaz");
        }
        if (dto.sal() == null || dto.sal().signum() < 0) {
            throw new IllegalArgumentException("Maas bos ya da negatif olamaz");
        }
        if (dto.comm() != null && dto.comm().signum() < 0) {
            throw new IllegalArgumentException("Komisyon negatif olamaz");
        }

        Optional<EmpDto> found = findOne(empno);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        EmpDto old = found.get();
        String img = dto.img() != null ? dto.img() : old.img();

        EmpDto updated = new EmpDto(old.empno(), dto.ename(), old.job(), old.managerName(),
                old.hireDate(), dto.sal(), dto.comm(), old.dname(), img);
        saveToRedis(updated, new EmpUpdateDto(dto.ename(), dto.sal(), dto.comm(), img));
        return Optional.of(updated);
    }

    public Optional<EmpDto> updateImage(Integer empno, String fileName) {
        Optional<EmpDto> found = findOne(empno);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        EmpDto old = found.get();
        EmpDto updated = new EmpDto(old.empno(), old.ename(), old.job(), old.managerName(),
                old.hireDate(), old.sal(), old.comm(), old.dname(), fileName);
        saveToRedis(updated, new EmpUpdateDto(old.ename(), old.sal(), old.comm(), fileName));
        return Optional.of(updated);
    }

    public boolean exists(Integer empno) {
        return findOne(empno).isPresent();
    }
}