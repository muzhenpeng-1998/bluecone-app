package com.bluecone.app.infra.configcenter.layer;

import com.bluecone.app.core.config.ConfigContext;

import java.util.Optional;

/**
 * A single configuration layer that can provide raw values based on scope.
 */
public interface ConfigLayer {

    Optional<String> get(String configKey, ConfigContext context);
}
