package com.sky.service.impl;

import com.sky.service.CacheService;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Service
public class RedisCacheServiceImpl implements CacheService {

    private static final long SCAN_BATCH_SIZE = 100L;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    @Override
    public void evictByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();

        try (Cursor<String> cursor = appRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }

        if (!keys.isEmpty()) {
            appRedisTemplate.delete(keys);
        }
    }
}
