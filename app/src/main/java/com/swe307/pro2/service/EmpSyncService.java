package com.swe307.pro2.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.swe307.pro2.dto.EmpUpdateDto;
import com.swe307.pro2.model.Emp;
import com.swe307.pro2.repository.EmpRepository;

import tools.jackson.databind.json.JsonMapper;

@Service
public class EmpSyncService {
    private static final Logger log = LoggerFactory.getLogger(EmpSyncService.class);

    private final EmpRepository _repo;
    private final StringRedisTemplate _redis;
    private final JsonMapper _json;

    public EmpSyncService(EmpRepository repo, StringRedisTemplate redis, JsonMapper json) {
        _repo = repo;
        _redis = redis;
        _json = json;
    }

    @Scheduled(fixedDelayString = "${emp.flush.interval-ms:30000}")
    public void flush() {
        Map<Object, Object> pending = _redis.opsForHash().entries(EmpService.PENDING_KEY);
        if (pending.isEmpty()) {
            return;
        }

        log.info("FLUSH basladi {} bekleyen kayit MySQL'e yaziliyor", pending.size());

        for (Map.Entry<Object, Object> entry : pending.entrySet()) {
            String field = (String) entry.getKey();
            String json = (String) entry.getValue();
            try {
                EmpUpdateDto dto = _json.readValue(json, EmpUpdateDto.class);
                Optional<Emp> found = _repo.findById(Integer.valueOf(field));

                if (found.isPresent()) {
                    Emp emp = found.get();
                    emp.setEname(dto.ename());
                    emp.setSal(dto.sal());
                    emp.setComm(dto.comm());
                    _repo.save(emp);
                    log.info("FLUSH: {} MySQL'e yazildi", field);

                } else {
                    log.warn("FLUSH: {} MySQL'de yok atlandi", field);
                }

                if (json.equals(_redis.opsForHash().get(EmpService.PENDING_KEY, field))) {
                    _redis.opsForHash().delete(EmpService.PENDING_KEY, field);
                }
            } catch (Exception e) {
                log.error("FLUSH hatasi({}), kayit bekleyenlerde kaldi", field, e);
            }
        }
    }
}
