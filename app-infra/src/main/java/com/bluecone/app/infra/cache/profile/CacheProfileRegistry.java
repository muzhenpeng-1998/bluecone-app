package com.bluecone.app.infra.cache.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 内存中的 Profile 注册表，集中定义并允许 yml 覆盖少量参数。
 */
@Component
@EnableConfigurationProperties(CacheProfileProperties.class)
public class CacheProfileRegistry {

    private static final Logger log = LoggerFactory.getLogger(CacheProfileRegistry.class);

    private final CacheProfileProperties properties;
    private final Map<CacheProfileName, CacheProfile> registry = new EnumMap<>(CacheProfileName.class);

    public CacheProfileRegistry(CacheProfileProperties properties) {
        this.properties = properties;
        loadDefaults();
        applyOverrides();
    }

    public CacheProfile getProfile(CacheProfileName name) {
        CacheProfile profile = registry.get(name);
        if (profile == null) {
            throw new IllegalArgumentException("CacheProfile not found: " + name);
        }
        return profile;
    }

    private void loadDefaults() {
        registry.put(CacheProfileName.USER_PROFILE, CacheProfile.builder(CacheProfileName.USER_PROFILE)
                .domain("user")
                .ttl(Duration.ofMinutes(10))
                .cacheNull(true)
                .strongConsistency(true)
                .build());

        registry.put(CacheProfileName.ORDER_DETAIL, CacheProfile.builder(CacheProfileName.ORDER_DETAIL)
                .domain("order")
                .ttl(Duration.ofMinutes(3))
                .cacheNull(true)
                .strongConsistency(true)
                .build());

        registry.put(CacheProfileName.TENANT_CONFIG, CacheProfile.builder(CacheProfileName.TENANT_CONFIG)
                .domain("tenant-config")
                .ttl(Duration.ofMinutes(30))
                .cacheNull(false)
                .strongConsistency(true)
                .build());

        registry.put(CacheProfileName.STORE_CONFIG, CacheProfile.builder(CacheProfileName.STORE_CONFIG)
                .domain("store-config")
                .ttl(Duration.ofMinutes(5))
                .cacheNull(false)
                .strongConsistency(true)
                .build());

        registry.put(CacheProfileName.STORE_BASE, CacheProfile.builder(CacheProfileName.STORE_BASE)
                .domain("store-base")
                .ttl(Duration.ofMinutes(3))
                .cacheNull(true)
                .strongConsistency(true)
                .build());

        registry.put(CacheProfileName.STORE_SNAPSHOT, CacheProfile.builder(CacheProfileName.STORE_SNAPSHOT)
                .domain("store-snapshot")
                .ttl(Duration.ofMinutes(2))
                .cacheNull(true)
                .strongConsistency(true)
                .build());
    }

    private void applyOverrides() {
        properties.getProfiles().forEach((kebabName, override) -> {
            CacheProfileName name = resolveProfileName(kebabName);
            CacheProfile existing = registry.get(name);
            if (existing == null) {
                return;
            }
            CacheProfile.Builder builder = CacheProfile.builder(name)
                    .domain(existing.domain())
                    .ttl(existing.ttl())
                    .cacheNull(existing.cacheNull())
                    .strongConsistency(existing.strongConsistency())
                    .hotKey(existing.hotKey());
            if (override.ttlOrNull() != null) {
                builder.ttl(override.ttlOrNull());
            }
            if (override.cacheNull() != null) {
                builder.cacheNull(override.cacheNull());
            }
            if (override.strongConsistency() != null) {
                builder.strongConsistency(override.strongConsistency());
            }
            if (override.hotKey() != null) {
                builder.hotKey(override.hotKey());
            }
            CacheProfile updated = builder.build();
            registry.put(name, updated);
            log.info("cache.profile.override name={} ttl={} cacheNull={} strongConsistency={} hotKey={}",
                    name, updated.ttl(), updated.cacheNull(), updated.strongConsistency(), updated.hotKey());
        });
    }

    private CacheProfileName resolveProfileName(String kebabName) {
        String enumName = kebabName.toUpperCase(Locale.ROOT).replace('-', '_');
        return CacheProfileName.valueOf(enumName);
    }
}
