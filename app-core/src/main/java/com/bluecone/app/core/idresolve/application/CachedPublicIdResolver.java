package com.bluecone.app.core.idresolve.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.config.IdResolveProperties;
import com.bluecone.app.core.idresolve.spi.PublicIdFallbackLookup;
import com.bluecone.app.core.idresolve.spi.PublicIdL2Cache;
import com.bluecone.app.core.idresolve.spi.PublicIdL2CacheResult;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 带多级缓存的公共 ID 解析器。
 *
 * <p>L1：进程内 Caffeine，L2：Redis（可选），底层：bc_public_id_map 映射表，
 * 可选回退：业务主表查询。</p>
 */
public class CachedPublicIdResolver implements PublicIdResolver {

    private static final String METRIC_NAME = "bluecone.idresolve.total";

    private final PublicIdMapRepository repository;
    private final PublicIdL2Cache l2Cache;
    private final boolean l2Enabled;
    private final PublicIdCodec publicIdCodec;
    private final IdResolveProperties properties;
    private final List<PublicIdFallbackLookup> fallbackLookups;
    private final Clock clock;
    private final Cache<String, CacheEntry> l1Cache;
    private final Counter hitL1Counter;
    private final Counter hitL2Counter;
    private final Counter hitDbCounter;
    private final Counter missCounter;
    private final Counter invalidCounter;

    public CachedPublicIdResolver(PublicIdMapRepository repository,
                                  PublicIdL2Cache l2Cache,
                                  PublicIdCodec publicIdCodec,
                                  IdResolveProperties properties,
                                  List<PublicIdFallbackLookup> fallbackLookups,
                                  MeterRegistry meterRegistry,
                                  Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.l2Cache = l2Cache;
        this.publicIdCodec = Objects.requireNonNull(publicIdCodec, "publicIdCodec");
        this.properties = (properties != null ? properties : new IdResolveProperties());
        this.fallbackLookups = (fallbackLookups != null ? fallbackLookups : List.of());
        this.clock = (clock != null ? clock : Clock.systemUTC());
        this.l2Enabled = this.properties.getCache().isL2Enabled() && l2Cache != null;
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(this.properties.getCache().getL1MaxSize())
                .build();

        if (meterRegistry != null) {
            this.hitL1Counter = meterRegistry.counter(METRIC_NAME, "result", "hit_l1");
            this.hitL2Counter = meterRegistry.counter(METRIC_NAME, "result", "hit_l2");
            this.hitDbCounter = meterRegistry.counter(METRIC_NAME, "result", "hit_db");
            this.missCounter = meterRegistry.counter(METRIC_NAME, "result", "miss");
            this.invalidCounter = meterRegistry.counter(METRIC_NAME, "result", "invalid");
        } else {
            this.hitL1Counter = null;
            this.hitL2Counter = null;
            this.hitDbCounter = null;
            this.missCounter = null;
            this.invalidCounter = null;
        }
    }

    @Override
    public ResolveResult resolve(ResolveKey key) {
        Objects.requireNonNull(key, "key");
        ValidationResult validation = validate(key.tenantId(), key.type(), key.publicId());
        if (!validation.valid()) {
            recordInvalid();
            return new ResolveResult(false, false, null, key.publicId(), validation.reason());
        }

        String cacheKey = buildCacheKey(key.tenantId(), key.type(), key.publicId());
        Duration l1Ttl = properties.getCache().getL1Ttl();
        Duration negativeTtl = properties.getCache().getNegativeTtl();
        Duration l2Ttl = properties.getCache().getL2Ttl();

        CacheEntry cached = l1Cache.getIfPresent(cacheKey);
        if (cached != null && !cached.isExpired(clock.instant())) {
            if (cached.negative()) {
                recordHitL1();
                return new ResolveResult(true, false, null, key.publicId(), ResolveResult.REASON_NOT_FOUND);
            }
            recordHitL1();
            return new ResolveResult(true, true, cached.internalId(), key.publicId(), "HIT_L1");
        }

        if (l2Enabled) {
            PublicIdL2CacheResult l2Result = l2Cache.get(key.tenantId(), key.type(), key.publicId());
            if (l2Result.hit()) {
                recordHitL2();
                if (l2Result.negative()) {
                    putL1Negative(cacheKey, negativeTtl);
                    return new ResolveResult(true, false, null, key.publicId(), ResolveResult.REASON_NOT_FOUND);
                }
                Ulid128 internalId = l2Result.internalId();
                putL1Positive(cacheKey, internalId, l1Ttl);
                return new ResolveResult(true, true, internalId, key.publicId(), "HIT_L2");
            }
        }

        Optional<Ulid128> dbHit = repository.findInternalId(key.tenantId(), key.type().name(), key.publicId());
        if (dbHit.isPresent()) {
            recordHitDb();
            Ulid128 internalId = dbHit.get();
            putL1Positive(cacheKey, internalId, l1Ttl);
            if (l2Enabled) {
                l2Cache.putPositive(key.tenantId(), key.type(), key.publicId(), internalId, l2Ttl);
            }
            return new ResolveResult(true, true, internalId, key.publicId(), "HIT_DB");
        }

        Optional<Ulid128> fallback = Optional.empty();
        if (properties.isFallbackToBizTable()) {
            fallback = findFallback(key.tenantId(), key.type(), key.publicId());
        }
        if (fallback.isPresent()) {
            recordHitDb();
            Ulid128 internalId = fallback.get();
            putL1Positive(cacheKey, internalId, l1Ttl);
            if (l2Enabled) {
                l2Cache.putPositive(key.tenantId(), key.type(), key.publicId(), internalId, l2Ttl);
            }
            return new ResolveResult(true, true, internalId, key.publicId(), "HIT_FALLBACK");
        }

        recordMiss();
        putL1Negative(cacheKey, negativeTtl);
        if (l2Enabled) {
            l2Cache.putNegative(key.tenantId(), key.type(), key.publicId(), negativeTtl);
        }
        return new ResolveResult(true, false, null, key.publicId(), ResolveResult.REASON_NOT_FOUND);
    }

    @Override
    public Map<String, ResolveResult> resolveBatch(long tenantId, ResourceType type, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Objects.requireNonNull(type, "type");

        Duration l1Ttl = properties.getCache().getL1Ttl();
        Duration negativeTtl = properties.getCache().getNegativeTtl();
        Duration l2Ttl = properties.getCache().getL2Ttl();

        Map<String, ResolveResult> results = new HashMap<>();
        Set<String> remaining = new HashSet<>();

        for (String publicId : publicIds) {
            if (results.containsKey(publicId)) {
                continue;
            }
            ValidationResult v = validate(tenantId, type, publicId);
            if (!v.valid()) {
                recordInvalid();
                results.put(publicId, new ResolveResult(false, false, null, publicId, v.reason()));
                continue;
            }
            String cacheKey = buildCacheKey(tenantId, type, publicId);
            CacheEntry cached = l1Cache.getIfPresent(cacheKey);
            if (cached != null && !cached.isExpired(clock.instant())) {
                if (cached.negative()) {
                    recordHitL1();
                    results.put(publicId, new ResolveResult(true, false, null, publicId, ResolveResult.REASON_NOT_FOUND));
                } else {
                    recordHitL1();
                    results.put(publicId, new ResolveResult(true, true, cached.internalId(), publicId, "HIT_L1"));
                }
            } else {
                remaining.add(publicId);
            }
        }

        if (remaining.isEmpty()) {
            return results;
        }

        if (l2Enabled) {
            List<String> l2Keys = new ArrayList<>(remaining);
            Map<String, PublicIdL2CacheResult> l2Results = l2Cache.getBatch(tenantId, type, l2Keys);
            for (String publicId : l2Keys) {
                PublicIdL2CacheResult r = l2Results.get(publicId);
                if (r == null || !r.hit()) {
                    continue;
                }
                String cacheKey = buildCacheKey(tenantId, type, publicId);
                recordHitL2();
                if (r.negative()) {
                    putL1Negative(cacheKey, negativeTtl);
                    results.put(publicId, new ResolveResult(true, false, null, publicId, ResolveResult.REASON_NOT_FOUND));
                } else {
                    Ulid128 internalId = r.internalId();
                    putL1Positive(cacheKey, internalId, l1Ttl);
                    results.put(publicId, new ResolveResult(true, true, internalId, publicId, "HIT_L2"));
                }
                remaining.remove(publicId);
            }
        }

        if (!remaining.isEmpty()) {
            int batchSize = Math.max(1, properties.getBatchMaxIn());
            List<String> remainingList = new ArrayList<>(remaining);
            Map<String, Ulid128> foundAll = new HashMap<>();
            for (int i = 0; i < remainingList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, remainingList.size());
                List<String> subList = remainingList.subList(i, end);
                Map<String, Ulid128> part = repository.findInternalIds(tenantId, type.name(), subList);
                if (part != null) {
                    foundAll.putAll(part);
                }
            }
            for (String publicId : new ArrayList<>(remaining)) {
                String cacheKey = buildCacheKey(tenantId, type, publicId);
                Ulid128 internalId = foundAll.get(publicId);
                if (internalId != null) {
                    recordHitDb();
                    putL1Positive(cacheKey, internalId, l1Ttl);
                    if (l2Enabled) {
                        l2Cache.putPositive(tenantId, type, publicId, internalId, l2Ttl);
                    }
                    results.put(publicId, new ResolveResult(true, true, internalId, publicId, "HIT_DB"));
                    remaining.remove(publicId);
                }
            }

            if (!remaining.isEmpty() && properties.isFallbackToBizTable()) {
                Map<String, Ulid128> fallbackFound = findFallbackBatch(tenantId, type, new ArrayList<>(remaining));
                for (String publicId : new ArrayList<>(remaining)) {
                    String cacheKey = buildCacheKey(tenantId, type, publicId);
                    Ulid128 internalId = fallbackFound.get(publicId);
                    if (internalId != null) {
                        recordHitDb();
                        putL1Positive(cacheKey, internalId, l1Ttl);
                        if (l2Enabled) {
                            l2Cache.putPositive(tenantId, type, publicId, internalId, l2Ttl);
                        }
                        results.put(publicId, new ResolveResult(true, true, internalId, publicId, "HIT_FALLBACK"));
                        remaining.remove(publicId);
                    }
                }
            }

            for (String publicId : remaining) {
                recordMiss();
                String cacheKey = buildCacheKey(tenantId, type, publicId);
                putL1Negative(cacheKey, negativeTtl);
                if (l2Enabled) {
                    l2Cache.putNegative(tenantId, type, publicId, negativeTtl);
                }
                results.put(publicId, new ResolveResult(true, false, null, publicId, ResolveResult.REASON_NOT_FOUND));
            }
        }

        return results;
    }

    private ValidationResult validate(long tenantId, ResourceType type, String publicId) {
        if (tenantId <= 0) {
            return new ValidationResult(false, "TENANT_INVALID");
        }
        if (type == null) {
            return new ValidationResult(false, "TYPE_INVALID");
        }
        if (publicId == null || publicId.isBlank()) {
            return new ValidationResult(false, ResolveResult.REASON_INVALID_FORMAT);
        }
        String trimmed = publicId.trim();
        try {
            var decoded = publicIdCodec.decode(trimmed);
            if (!type.prefix().equals(decoded.type())) {
                return new ValidationResult(false, ResolveResult.REASON_PREFIX_MISMATCH);
            }
        } catch (IllegalArgumentException ex) {
            return new ValidationResult(false, ResolveResult.REASON_INVALID_FORMAT);
        }
        return new ValidationResult(true, null);
    }

    private Optional<Ulid128> findFallback(long tenantId, ResourceType type, String publicId) {
        for (PublicIdFallbackLookup lookup : fallbackLookups) {
            if (lookup.supports(type)) {
                Optional<Ulid128> found = lookup.findInternalId(tenantId, type, publicId);
                if (found != null && found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private Map<String, Ulid128> findFallbackBatch(long tenantId, ResourceType type, List<String> publicIds) {
        for (PublicIdFallbackLookup lookup : fallbackLookups) {
            if (lookup.supports(type)) {
                Map<String, Ulid128> found = lookup.findInternalIds(tenantId, type, publicIds);
                if (found != null) {
                    return found;
                }
            }
        }
        return Collections.emptyMap();
    }

    private String buildCacheKey(long tenantId, ResourceType type, String publicId) {
        return tenantId + ":" + type.name() + ":" + publicId;
    }

    private void putL1Positive(String cacheKey, Ulid128 id, Duration ttl) {
        l1Cache.put(cacheKey, CacheEntry.positive(id, clock.instant().plus(ttl)));
    }

    private void putL1Negative(String cacheKey, Duration ttl) {
        l1Cache.put(cacheKey, CacheEntry.negative(clock.instant().plus(ttl)));
    }

    private void recordHitL1() {
        if (hitL1Counter != null) {
            hitL1Counter.increment();
        }
    }

    private void recordHitL2() {
        if (hitL2Counter != null) {
            hitL2Counter.increment();
        }
    }

    private void recordHitDb() {
        if (hitDbCounter != null) {
            hitDbCounter.increment();
        }
    }

    private void recordMiss() {
        if (missCounter != null) {
            missCounter.increment();
        }
    }

    private void recordInvalid() {
        if (invalidCounter != null) {
            invalidCounter.increment();
        }
    }

    private record CacheEntry(Ulid128 internalId, boolean negative, Instant expireAt) {

        static CacheEntry positive(Ulid128 id, Instant expireAt) {
            return new CacheEntry(id, false, expireAt);
        }

        static CacheEntry negative(Instant expireAt) {
            return new CacheEntry(null, true, expireAt);
        }

        boolean isExpired(Instant now) {
            return expireAt != null && now.isAfter(expireAt);
        }
    }

    private record ValidationResult(boolean valid, String reason) {
    }
}

