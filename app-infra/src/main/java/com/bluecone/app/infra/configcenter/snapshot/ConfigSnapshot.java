package com.bluecone.app.infra.configcenter.snapshot;

import java.util.Map;
import java.util.Optional;

/**
 * Immutable snapshot of resolved configuration for a given environment.
 */
public class ConfigSnapshot {

    private final Map<String, String> merged;

    public ConfigSnapshot(Map<String, String> merged) {
        this.merged = Map.copyOf(merged);
    }

    public Optional<String> get(String mergedKey) {
        return Optional.ofNullable(merged.get(mergedKey));
    }

    public Map<String, String> asMap() {
        return merged;
    }
}
