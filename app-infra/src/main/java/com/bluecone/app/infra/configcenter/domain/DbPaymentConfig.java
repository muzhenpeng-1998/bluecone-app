package com.bluecone.app.infra.configcenter.domain;

import com.bluecone.app.core.config.ConfigKey;
import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.core.config.domain.PaymentConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Payment domain configuration backed by ConfigCenter.
 */
@Service
public class DbPaymentConfig implements PaymentConfig {

    private static final int DEFAULT_PAYMENT_EXPIRE_MINUTES = 30;
    private static final boolean DEFAULT_PARTIAL_REFUND = false;
    private static final BigDecimal DEFAULT_MAX_REFUND_PER_DAY = BigDecimal.valueOf(1000);

    private final ConfigService configService;

    public DbPaymentConfig(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Duration paymentExpireAfter() {
        int minutes = configService.getInt(ConfigKey.PAYMENT_EXPIRE_MINUTES, DEFAULT_PAYMENT_EXPIRE_MINUTES);
        return Duration.ofMinutes(minutes);
    }

    @Override
    public boolean allowPartialRefund() {
        return configService.getBoolean(ConfigKey.PAYMENT_ALLOW_PARTIAL_REFUND, DEFAULT_PARTIAL_REFUND);
    }

    @Override
    public BigDecimal maxRefundAmountPerDay() {
        String raw = configService.getRaw(ConfigKey.PAYMENT_MAX_REFUND_PER_DAY).orElse(null);
        if (raw == null) {
            return DEFAULT_MAX_REFUND_PER_DAY;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_REFUND_PER_DAY;
        }
    }
}
