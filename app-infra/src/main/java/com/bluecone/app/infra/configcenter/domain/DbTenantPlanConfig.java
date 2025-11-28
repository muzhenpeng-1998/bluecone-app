package com.bluecone.app.infra.configcenter.domain;

import com.bluecone.app.core.config.ConfigKey;
import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.domain.TenantPlanConfig;
import org.springframework.stereotype.Service;

/**
 * Tenant plan configuration backed by ConfigCenter.
 */
@Service
public class DbTenantPlanConfig implements TenantPlanConfig {

    private static final String DEFAULT_PLAN = "FREE";
    private static final int DEFAULT_MAX_STORES = 1;
    private static final int DEFAULT_MAX_USERS = 5;

    private final ConfigService configService;

    public DbTenantPlanConfig(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String planLevel() {
        return configService.getRaw(ConfigKey.TENANT_PLAN_LEVEL).orElse(DEFAULT_PLAN);
    }

    @Override
    public boolean hasFeature(Feature feature) {
        return configService.getBoolean(feature.key(), feature.defaultValue());
    }

    @Override
    public int maxStores() {
        return configService.getInt(ConfigKey.TENANT_MAX_STORES, DEFAULT_MAX_STORES);
    }

    @Override
    public int maxUsers() {
        return configService.getInt(ConfigKey.TENANT_MAX_USERS, DEFAULT_MAX_USERS);
    }
}
