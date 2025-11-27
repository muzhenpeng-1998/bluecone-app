package com.bluecone.app.infra.cache.core;

import java.util.Optional;

/**
 * 内部值包装器，用于区分“真实 null”与“未命中”。
 */
final class CacheValueWrapper {

    private static final CacheValueWrapper NULL_VALUE = new CacheValueWrapper(NullValue.INSTANCE, true);

    private final Object value;
    private final boolean nullValue;

    private CacheValueWrapper(Object value, boolean nullValue) {
        this.value = value;
        this.nullValue = nullValue;
    }

    static CacheValueWrapper of(Object value) {
        if (value == null) {
            return NULL_VALUE;
        }
        return new CacheValueWrapper(value, false);
    }

    boolean isNullValue() {
        return nullValue;
    }

    <T> Optional<T> asType(Class<T> type) {
        if (nullValue) {
            @SuppressWarnings("unchecked")
            T casted = (T) NullValue.INSTANCE;
            return Optional.of(casted);
        }
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    Object raw() {
        return value;
    }
}
