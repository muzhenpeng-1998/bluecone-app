package com.bluecone.app.infra.configcenter.snapshot;

import com.bluecone.app.infra.redis.support.RedisEnvProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds current configuration snapshot and refreshes it on demand.
 */
@Component
public class ConfigSnapshotManager {

    private final ConfigSnapshotLoader loader;
    private final RedisEnvProvider envProvider;
    private final AtomicReference<ConfigSnapshot> current = new AtomicReference<>(new ConfigSnapshot(Map.of()));

    public ConfigSnapshotManager(ConfigSnapshotLoader loader, RedisEnvProvider envProvider) {
        this.loader = loader;
        this.envProvider = envProvider;
    }

    @PostConstruct
    public void init() {
        refreshAll();
    }

    public ConfigSnapshot current() {
        return current.get();
    }

    public void refreshAll() {
        String env = envProvider.getEnv();
        ConfigSnapshot snapshot = loader.load(env);
        current.set(snapshot);
    }
}
