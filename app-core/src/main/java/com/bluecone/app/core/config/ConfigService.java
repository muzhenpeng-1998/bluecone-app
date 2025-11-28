package com.bluecone.app.core.config;

import java.util.Optional;

/**
 * High-level configuration access interface used by domain code.
 */
public interface ConfigService {

    Optional<String> getRaw(ConfigKey key);

    boolean getBoolean(ConfigKey key, boolean defaultValue);

    int getInt(ConfigKey key, int defaultValue);

    long getLong(ConfigKey key, long defaultValue);

    <T> T getJson(ConfigKey key, Class<T> type, T defaultValue);
}
