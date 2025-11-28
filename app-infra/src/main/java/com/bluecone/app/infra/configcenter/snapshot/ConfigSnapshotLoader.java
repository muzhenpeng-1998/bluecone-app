package com.bluecone.app.infra.configcenter.snapshot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.config.ConfigScope;
import com.bluecone.app.infra.configcenter.entity.ConfigPropertyEntity;
import com.bluecone.app.infra.configcenter.mapper.ConfigPropertyMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads configuration snapshot for a given environment by merging layers into a single immutable map.
 */
@Component
public class ConfigSnapshotLoader {

    private final ConfigPropertyMapper mapper;

    public ConfigSnapshotLoader(ConfigPropertyMapper mapper) {
        this.mapper = mapper;
    }

    public ConfigSnapshot load(String env) {
        String targetEnv = env == null ? "" : env;
        LambdaQueryWrapper<ConfigPropertyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigPropertyEntity::getEnabled, Boolean.TRUE)
                .and(w -> w.isNull(ConfigPropertyEntity::getEnv)
                        .or()
                        .eq(ConfigPropertyEntity::getEnv, targetEnv));
        List<ConfigPropertyEntity> rows = mapper.selectList(wrapper);

        Map<String, SnapshotValue> merged = new HashMap<>();
        for (ConfigPropertyEntity row : rows) {
            int priority = priority(row);
            Long tenantId = row.getTenantId() == null ? 0L : row.getTenantId();
            String mergedKey = buildMergedKey(row.getConfigKey(), targetEnv, tenantId);
            SnapshotValue current = merged.get(mergedKey);
            if (current == null || priority > current.priority) {
                merged.put(mergedKey, new SnapshotValue(priority, row.getConfigValue()));
            }
        }

        Map<String, String> result = new HashMap<>();
        merged.forEach((k, v) -> result.put(k, v.value));
        return new ConfigSnapshot(result);
    }

    public static String buildMergedKey(String configKey, String env, Long tenantId) {
        long tenant = tenantId == null ? 0L : tenantId;
        String resolvedEnv = env == null ? "" : env;
        return configKey + "|" + resolvedEnv + "|" + tenant;
    }

    private int priority(ConfigPropertyEntity entity) {
        String scope = entity.getScope();
        boolean hasEnv = entity.getEnv() != null && !entity.getEnv().isEmpty();
        if (ConfigScope.TENANT.name().equalsIgnoreCase(scope) && hasEnv) {
            return 4;
        }
        if (ConfigScope.TENANT.name().equalsIgnoreCase(scope)) {
            return 3;
        }
        if (ConfigScope.ENV.name().equalsIgnoreCase(scope)) {
            return 2;
        }
        return 1;
    }

    private record SnapshotValue(int priority, String value) {
    }
}
