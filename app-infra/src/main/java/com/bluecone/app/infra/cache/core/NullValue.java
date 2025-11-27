package com.bluecone.app.infra.cache.core;

/**
 * Sentinel representing a cached null to avoid重复加载。
 */
final class NullValue {
    static final NullValue INSTANCE = new NullValue();

    private NullValue() {
    }

    @Override
    public String toString() {
        return "NullValue";
    }
}
