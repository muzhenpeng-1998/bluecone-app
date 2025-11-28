package com.bluecone.app.infra.configcenter.domain;

import com.bluecone.app.core.config.ConfigKey;
import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.core.config.domain.OrderConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Order domain configuration backed by ConfigCenter.
 */
@Service
public class DbOrderConfig implements OrderConfig {

    private static final boolean DEFAULT_NEW_ENGINE = false;
    private static final int DEFAULT_PAYMENT_TIMEOUT_SECONDS = 900;
    private static final int DEFAULT_MAX_ITEMS = 99;
    private static final boolean DEFAULT_ALLOW_CROSS_DAY = false;

    private final ConfigService configService;

    public DbOrderConfig(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public boolean isNewEngineEnabled() {
        return configService.getBoolean(ConfigKey.FEATURE_NEW_ORDER_ENGINE, DEFAULT_NEW_ENGINE);
    }

    @Override
    public Duration paymentTimeout() {
        int seconds = configService.getInt(ConfigKey.ORDER_PAYMENT_TIMEOUT, DEFAULT_PAYMENT_TIMEOUT_SECONDS);
        return Duration.ofSeconds(seconds);
    }

    @Override
    public int maxItemsPerOrder() {
        return configService.getInt(ConfigKey.ORDER_MAX_ITEMS, DEFAULT_MAX_ITEMS);
    }

    @Override
    public boolean allowCrossDayOrder() {
        return configService.getBoolean(ConfigKey.ORDER_ALLOW_CROSS_DAY, DEFAULT_ALLOW_CROSS_DAY);
    }
}
