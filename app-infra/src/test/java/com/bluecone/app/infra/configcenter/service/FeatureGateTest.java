package com.bluecone.app.infra.configcenter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluecone.app.core.config.ConfigKey;
import com.bluecone.app.core.config.ConfigService;
import com.bluecone.app.core.config.Feature;
import org.junit.jupiter.api.Test;

class FeatureGateTest {

    private final ConfigService configService = mock(ConfigService.class);
    private final DefaultFeatureGate featureGate = new DefaultFeatureGate(configService);

    @Test
    void delegatesToConfigService() {
        when(configService.getBoolean(eq(ConfigKey.FEATURE_NEW_ORDER_ENGINE), eq(false))).thenReturn(true);

        assertThat(featureGate.isOn(Feature.NEW_ORDER_ENGINE)).isTrue();
    }

    @Test
    void fallsBackToDefaultWhenMissing() {
        when(configService.getBoolean(eq(ConfigKey.FEATURE_BILLING_V2), eq(false))).thenReturn(false);

        assertThat(featureGate.isOn(Feature.BILLING_V2)).isFalse();
    }
}
