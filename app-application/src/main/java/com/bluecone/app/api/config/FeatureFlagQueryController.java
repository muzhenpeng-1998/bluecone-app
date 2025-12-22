package com.bluecone.app.api.config;

import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.FeatureGate;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Simple feature flag query endpoints for debugging or gateway checks.
 */
@Tag(name = "🛠️ 开发调试 > 配置调试", description = "功能开关查询接口")
@RestController
@RequestMapping("/api/config/features")
public class FeatureFlagQueryController {

    private final FeatureGate featureGate;

    public FeatureFlagQueryController(FeatureGate featureGate) {
        this.featureGate = featureGate;
    }

    @GetMapping("/new-order-engine")
    public Map<String, Boolean> newOrderEngine() {
        return Map.of("enabled", featureGate.isOn(Feature.NEW_ORDER_ENGINE));
    }

    @GetMapping("/{featureName}")
    public Map<String, Boolean> anyFeature(@PathVariable String featureName) {
        Feature feature = Feature.valueOf(featureName.trim().toUpperCase(Locale.ROOT));
        return Map.of("enabled", featureGate.isOn(feature));
    }
}
