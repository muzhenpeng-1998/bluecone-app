package com.bluecone.app.infra.configcenter.service;

import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.FeatureGate;
import org.springframework.stereotype.Service;

/**
 * Default feature gate implementation backed by ConfigService.
 */
@Service
public class DefaultFeatureGate implements FeatureGate {

    private final ConfigService configService;

    public DefaultFeatureGate(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public boolean isOn(Feature feature) {
        return configService.getBoolean(feature.key(), feature.defaultValue());
    }
}
