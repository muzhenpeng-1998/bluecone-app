package com.bluecone.app.service;

import com.bluecone.app.infra.cache.annotation.CacheEvict;
import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo 缓存用例：用内存 Map 模拟数据源，便于验证 @Cached / @CacheEvict。
 */
@Service
public class DemoCacheService {

    private final Map<Long, DemoValue> repository = new ConcurrentHashMap<>();

    @Cached(profile = CacheProfileName.ORDER_DETAIL, key = "#id")
    public DemoValue load(Long id) {
        // 模拟真实查询：生成带时间戳的对象，方便观察是否重复加载
        return repository.computeIfAbsent(id, key -> new DemoValue(key, "fresh-" + Instant.now().toEpochMilli()));
    }

    @CacheEvict(profile = CacheProfileName.ORDER_DETAIL, key = "#id")
    public DemoValue update(Long id, String payload) {
        DemoValue updated = new DemoValue(id, payload == null ? "updated-" + Instant.now().toEpochMilli() : payload);
        repository.put(id, updated);
        return updated;
    }

    public record DemoValue(Long id, String payload) {
    }
}
