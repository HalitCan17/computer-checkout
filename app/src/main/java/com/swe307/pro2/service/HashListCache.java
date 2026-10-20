package com.swe307.pro2.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.json.JsonMapper;

// Bir tablonun tum satirlarini tek bir Redis hash'inde tutar: alan = id, deger = satirin JSON'u.
// Ogrenci ve bilgisayar listeleri bu sinifla calisir.
public class HashListCache<T> {

    private static final Logger log = LoggerFactory.getLogger(HashListCache.class);
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate _redis;
    private final JsonMapper _json;
    private final String _key;
    private final Class<T> _type;
    private final Function<T, Long> _idOf;
    private final Supplier<List<T>> _loader;   // Redis bossa MySQL'den listeyi getirir (id sirali)

    public HashListCache(StringRedisTemplate redis, JsonMapper json, String key, Class<T> type,
                         Function<T, Long> idOf, Supplier<List<T>> loader) {
        _redis = redis;
        _json = json;
        _key = key;
        _type = type;
        _idOf = idOf;
        _loader = loader;
    }

    // Redis'te varsa oradan (HIT), yoksa MySQL'den doldurup doner (MISS)
    public List<T> findAll() {
        if (Boolean.TRUE.equals(_redis.hasKey(_key))) {
            log.info("CACHE HIT: {} Redis'ten donuldu", _key);
            return _redis.opsForHash().values(_key).stream()
                    .map(v -> _json.readValue((String) v, _type))
                    .sorted((a, b) -> _idOf.apply(a).compareTo(_idOf.apply(b)))
                    .toList();
        }
        log.info("CACHE MISS: {} Redis'te yok, MySQL'e gidildi", _key);
        List<T> list = _loader.get();
        if (!list.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            for (T item : list) {
                map.put(String.valueOf(_idOf.apply(item)), _json.writeValueAsString(item));
            }
            _redis.opsForHash().putAll(_key, map);
            _redis.expire(_key, TTL);
        }
        return list;
    }

    // MySQL'e yazildiktan sonra cagrilir. Liste Redis'te yoksa yazmaz: tek kayitlik yarim liste olusmasin
    public void put(T item) {
        try {
            if (Boolean.TRUE.equals(_redis.hasKey(_key))) {
                _redis.opsForHash().put(_key, String.valueOf(_idOf.apply(item)), _json.writeValueAsString(item));
            }
        } catch (Exception ex) {
            clear();
        }
    }

    public void remove(Long id) {
        try {
            _redis.opsForHash().delete(_key, String.valueOf(id));
        } catch (Exception ex) {
            clear();
        }
    }

    // Hata durumunda: listeyi sil, sonraki okuma MySQL'den taze yuklesin
    public void clear() {
        try {
            _redis.delete(_key);
        } catch (Exception ignore) {
            // Redis kapali: liste TTL ile kendiliginden bitecek
        }
    }
}
