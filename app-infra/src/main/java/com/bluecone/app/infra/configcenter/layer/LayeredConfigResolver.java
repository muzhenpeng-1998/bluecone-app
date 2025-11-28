package com.bluecone.app.infra.configcenter.layer;

import com.bluecone.app.core.config.ConfigContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Resolves configuration by traversing layers in priority order.
 */
@Component
public class LayeredConfigResolver {

    private final List<ConfigLayer> layers;

    public LayeredConfigResolver(TenantEnvConfigLayer tenantEnv,
                                 TenantGlobalConfigLayer tenantGlobal,
                                 EnvConfigLayer envLayer,
                                 SystemConfigLayer systemLayer) {
        this.layers = Arrays.asList(tenantEnv, tenantGlobal, envLayer, systemLayer);
    }

    public Optional<String> resolve(String configKey, ConfigContext context) {
        for (ConfigLayer layer : layers) {
            Optional<String> value = layer.get(configKey, context);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }
}
