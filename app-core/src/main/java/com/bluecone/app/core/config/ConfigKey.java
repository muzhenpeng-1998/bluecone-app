package com.bluecone.app.core.config;

import java.util.Objects;

/**
 * Typed wrapper around raw configuration keys to avoid scattering literals in business code.
 */
public final class ConfigKey {

    public static final ConfigKey FEATURE_NEW_ORDER_ENGINE = ConfigKey.of("feature.new_order_engine.enabled");
    public static final ConfigKey FEATURE_BILLING_V2 = ConfigKey.of("feature.billing.v2.enabled");
    public static final ConfigKey ORDER_PAYMENT_TIMEOUT = ConfigKey.of("order.payment.timeout.seconds");
    public static final ConfigKey ORDER_MAX_ITEMS = ConfigKey.of("order.max.items");
    public static final ConfigKey ORDER_ALLOW_CROSS_DAY = ConfigKey.of("order.allow_cross_day");
    public static final ConfigKey TENANT_PLAN_LEVEL = ConfigKey.of("tenant.plan.level");
    public static final ConfigKey TENANT_MAX_STORES = ConfigKey.of("tenant.max.stores");
    public static final ConfigKey TENANT_MAX_USERS = ConfigKey.of("tenant.max.users");
    public static final ConfigKey PAYMENT_EXPIRE_MINUTES = ConfigKey.of("payment.expire.minutes");
    public static final ConfigKey PAYMENT_ALLOW_PARTIAL_REFUND = ConfigKey.of("payment.allow_partial_refund");
    public static final ConfigKey PAYMENT_MAX_REFUND_PER_DAY = ConfigKey.of("payment.max_refund_amount_per_day");

    private final String value;

    private ConfigKey(String value) {
        this.value = value;
    }

    public static ConfigKey of(String value) {
        Objects.requireNonNull(value, "config key must not be null");
        return new ConfigKey(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
