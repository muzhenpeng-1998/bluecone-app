package com.bluecone.app.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.infra.cacheinval.CacheInvalidationLogDO;
import com.bluecone.app.infra.cacheinval.CacheInvalidationLogMapper;
import com.bluecone.app.ops.api.dto.cacheinval.CacheInvalItem;
import com.bluecone.app.ops.api.dto.cacheinval.CacheInvalSummary;
import com.bluecone.app.ops.api.dto.drill.PageResult;
import com.bluecone.app.ops.config.BlueconeOpsProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultCacheInvalidationOpsService implements CacheInvalidationOpsService {

    private final CacheInvalidationLogMapper mapper;
    private final BlueconeOpsProperties properties;

    private final ConcurrentMap<String, Cached<?>> cache = new ConcurrentHashMap<>();

    public DefaultCacheInvalidationOpsService(CacheInvalidationLogMapper mapper,
                                              BlueconeOpsProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public CacheInvalSummary getSummary(String window) {
        Duration dur = parseWindow(window);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minus(dur);

        String cacheKey = "cacheInvalSummary|" + window;
        CacheInvalSummary cached = getCached(cacheKey, CacheInvalSummary.class);
        if (cached != null) {
            return cached;
        }

        List<CacheInvalidationLogDO> logs = mapper.selectList(new LambdaQueryWrapper<CacheInvalidationLogDO>()
                .ge(CacheInvalidationLogDO::getOccurredAt, from)
                .le(CacheInvalidationLogDO::getOccurredAt, now));

        long total = logs.size();

        Map<String, Long> byScope = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getScope() == null ? "UNKNOWN" : l.getScope(),
                        Collectors.counting()));

        Map<String, Long> byNamespace = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getNamespace() == null ? "UNKNOWN" : l.getNamespace(),
                        Collectors.counting()));

        Map<Long, Long> byTenant = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getTenantId() == null ? 0L : l.getTenantId(),
                        Collectors.counting()));

        List<CacheInvalSummary.ScopeStat> scopeStats = byScope.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new CacheInvalSummary.ScopeStat(e.getKey(), e.getValue()))
                .toList();

        List<CacheInvalSummary.NamespaceStat> nsStats = byNamespace.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new CacheInvalSummary.NamespaceStat(e.getKey(), e.getValue()))
                .toList();

        List<CacheInvalSummary.TenantStat> tenantStats = byTenant.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new CacheInvalSummary.TenantStat(e.getKey(), e.getValue()))
                .toList();

        // storm detection: last 1 minute
        Duration stormWindow = Duration.ofMinutes(1);
        LocalDateTime stormFrom = now.minus(stormWindow);
        List<CacheInvalidationLogDO> stormLogs = mapper.selectList(new LambdaQueryWrapper<CacheInvalidationLogDO>()
                .ge(CacheInvalidationLogDO::getOccurredAt, stormFrom)
                .le(CacheInvalidationLogDO::getOccurredAt, now));

        int threshold = 300;
        Map<String, Long> grouped = stormLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> (l.getTenantId() == null ? 0L : l.getTenantId()) + "|" +
                                (l.getScope() == null ? "UNKNOWN" : l.getScope()) + "|" +
                                (l.getNamespace() == null ? "UNKNOWN" : l.getNamespace()),
                        Collectors.counting()));

        List<CacheInvalSummary.StormItem> storms = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            long count = e.getValue();
            if (count <= threshold) {
                continue;
            }
            String[] parts = e.getKey().split("\\|", 3);
            long tenantId = Long.parseLong(parts[0]);
            String scope = parts[1];
            String namespace = parts.length > 2 ? parts[2] : "UNKNOWN";
            storms.add(new CacheInvalSummary.StormItem(
                    tenantId,
                    scope,
                    namespace,
                    count,
                    threshold
            ));
        }

        Map<String, Long> byDecision = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getDecision() == null ? "UNKNOWN" : l.getDecision(),
                        Collectors.counting()));

        List<CacheInvalSummary.DecisionStat> decisionStats = byDecision.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new CacheInvalSummary.DecisionStat(e.getKey(), e.getValue()))
                .toList();

        CacheInvalSummary summary = new CacheInvalSummary(total, scopeStats, nsStats, tenantStats, storms, decisionStats);
        putCached(cacheKey, summary);
        return summary;
    }

    @Override
    public PageResult<CacheInvalItem> listRecent(String window,
                                                 String cursor,
                                                 int limit,
                                                 Long tenantId,
                                                 String scope,
                                                 String namespace) {
        int effectiveLimit = clampLimit(limit);
        Duration dur = parseWindow(window);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minus(dur);

        LambdaQueryWrapper<CacheInvalidationLogDO> wrapper = new LambdaQueryWrapper<CacheInvalidationLogDO>()
                .ge(CacheInvalidationLogDO::getOccurredAt, from)
                .le(CacheInvalidationLogDO::getOccurredAt, now)
                .orderByDesc(CacheInvalidationLogDO::getId)
                .last("LIMIT " + effectiveLimit);

        Long beforeId = parseCursor(cursor);
        if (beforeId != null && beforeId > 0) {
            wrapper.lt(CacheInvalidationLogDO::getId, beforeId);
        }
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(CacheInvalidationLogDO::getTenantId, tenantId);
        }
        if (StringUtils.hasText(scope)) {
            wrapper.eq(CacheInvalidationLogDO::getScope, scope.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(namespace)) {
            wrapper.eq(CacheInvalidationLogDO::getNamespace, namespace.trim());
        }

        List<CacheInvalidationLogDO> rows = mapper.selectList(wrapper);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        List<CacheInvalItem> items = rows.stream()
                .map(r -> new CacheInvalItem(
                        r.getId() == null ? -1L : r.getId(),
                        r.getOccurredAt() != null ? r.getOccurredAt().format(formatter) : null,
                        r.getTenantId() == null ? 0L : r.getTenantId(),
                        r.getScope(),
                        r.getNamespace(),
                        r.getEventId(),
                        r.getKeysCount() == null ? 0 : r.getKeysCount(),
                        r.getKeySamples(),
                        r.getConfigVersion(),
                        r.getTransport(),
                        r.getInstanceId(),
                        r.getResult(),
                        r.getDecision(),
                        r.getStormMode() != null && r.getStormMode() == 1,
                        r.getEpoch()
                ))
                .collect(Collectors.toList());

        String nextCursor = computeNextCursor(items.stream()
                .map(CacheInvalItem::id)
                .collect(Collectors.toList()));
        return new PageResult<>(items, nextCursor, effectiveLimit);
    }

    private Duration parseWindow(String window) {
        if (window == null || window.isBlank()) {
            return Duration.ofMinutes(5);
        }
        String w = window.trim().toLowerCase(Locale.ROOT);
        if (w.endsWith("m")) {
            String num = w.substring(0, w.length() - 1);
            try {
                return Duration.ofMinutes(Long.parseLong(num));
            } catch (NumberFormatException ignored) {
            }
        }
        if (w.endsWith("h")) {
            String num = w.substring(0, w.length() - 1);
            try {
                return Duration.ofHours(Long.parseLong(num));
            } catch (NumberFormatException ignored) {
            }
        }
        return Duration.ofMinutes(5);
    }

    private int clampLimit(int limit) {
        int max = properties.getMaxPageSize() > 0 ? properties.getMaxPageSize() : 100;
        int effective = limit <= 0 ? 50 : limit;
        if (effective > max) {
            effective = max;
        }
        return effective;
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String computeNextCursor(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        long min = ids.stream()
                .min(Comparator.naturalOrder())
                .orElse(-1L);
        return min > 0 ? Long.toString(min) : null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, Class<T> type) {
        Cached<?> c = cache.get(key);
        if (c == null) {
            return null;
        }
        if (c.expiresAt().isBefore(LocalDateTime.now())) {
            cache.remove(key);
            return null;
        }
        return (T) c.value();
    }

    private void putCached(String key, Object value) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(properties.getDrillCacheTtl());
        cache.put(key, new Cached<>(value, expiresAt));
    }

    private record Cached<T>(T value, LocalDateTime expiresAt) {
    }
}
