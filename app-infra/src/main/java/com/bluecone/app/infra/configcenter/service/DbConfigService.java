package com.bluecone.app.infra.configcenter.service;

import com.bluecone.app.core.config.ConfigContext;
import com.bluecone.app.core.config.ConfigKey;
import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.infra.configcenter.snapshot.ConfigSnapshot;
import com.bluecone.app.infra.configcenter.snapshot.ConfigSnapshotLoader;
import com.bluecone.app.infra.configcenter.snapshot.ConfigSnapshotManager;
import com.bluecone.app.infra.redis.support.RedisEnvProvider;
import com.bluecone.app.infra.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Database backed ConfigService using immutable snapshots for fast reads.
 */
@Service
public class DbConfigService implements ConfigService {

    private final ConfigSnapshotManager snapshotManager;
    private final RedisEnvProvider envProvider;
    private final ObjectMapper objectMapper;

    public DbConfigService(ConfigSnapshotManager snapshotManager,
                           RedisEnvProvider envProvider,
                           ObjectMapper objectMapper) {
        this.snapshotManager = snapshotManager;
        this.envProvider = envProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> getRaw(ConfigKey key) {
        String env = envProvider.getEnv();
        Long tenantId = resolveTenantId();
        ConfigSnapshot snapshot = snapshotManager.current();
        String mergedKey = ConfigSnapshotLoader.buildMergedKey(key.value(), env, tenantId);
        return snapshot.get(mergedKey);
    }

    @Override
    public boolean getBoolean(ConfigKey key, boolean defaultValue) {
        return getRaw(key)
                .map(v -> "true".equalsIgnoreCase(v) || "1".equals(v))
                .orElse(defaultValue);
    }

    @Override
    public int getInt(ConfigKey key, int defaultValue) {
        return getRaw(key)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    public long getLong(ConfigKey key, long defaultValue) {
        return getRaw(key)
                .map(v -> {
                    try {
                        return Long.parseLong(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    public <T> T getJson(ConfigKey key, Class<T> type, T defaultValue) {
        return getRaw(key)
                .map(v -> {
                    try {
                        return objectMapper.readValue(v, type);
                    } catch (JsonProcessingException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private Long resolveTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantIdStr)) {
            return null;
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
