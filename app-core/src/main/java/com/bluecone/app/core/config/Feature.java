package com.bluecone.app.core.config;

/**
 * Enumeration of feature flags exposed to business code.
 */
public enum Feature {
    NEW_ORDER_ENGINE(ConfigKey.FEATURE_NEW_ORDER_ENGINE, false),
    BILLING_V2(ConfigKey.FEATURE_BILLING_V2, false);

    private final ConfigKey key;
    private final boolean defaultValue;

    Feature(ConfigKey key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public ConfigKey key() {
        return key;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
