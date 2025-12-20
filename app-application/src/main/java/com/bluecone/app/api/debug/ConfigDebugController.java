package com.bluecone.app.api.config;

import com.bluecone.app.core.config.ConfigContext;
import com.bluecone.app.infra.configcenter.layer.EnvConfigLayer;
import com.bluecone.app.infra.configcenter.layer.LayeredConfigResolver;
import com.bluecone.app.infra.configcenter.layer.SystemConfigLayer;
import com.bluecone.app.infra.configcenter.layer.TenantEnvConfigLayer;
import com.bluecone.app.infra.configcenter.layer.TenantGlobalConfigLayer;
import com.bluecone.app.infra.redis.support.RedisEnvProvider;
import com.bluecone.app.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Developer-friendly debug endpoint to inspect resolution details.
 */
@Hidden
@Tag(name = "Internal - Config", description = "配置调试接口（仅开发环境）")
@RestController
@RequestMapping("/api/dev/config")
public class ConfigDebugController {

    private final TenantEnvConfigLayer tenantEnvConfigLayer;
    private final TenantGlobalConfigLayer tenantGlobalConfigLayer;
    private final EnvConfigLayer envConfigLayer;
    private final SystemConfigLayer systemConfigLayer;
    private final LayeredConfigResolver resolver;
    private final RedisEnvProvider envProvider;

    public ConfigDebugController(TenantEnvConfigLayer tenantEnvConfigLayer,
                                 TenantGlobalConfigLayer tenantGlobalConfigLayer,
                                 EnvConfigLayer envConfigLayer,
                                 SystemConfigLayer systemConfigLayer,
                                 LayeredConfigResolver resolver,
                                 RedisEnvProvider envProvider) {
        this.tenantEnvConfigLayer = tenantEnvConfigLayer;
        this.tenantGlobalConfigLayer = tenantGlobalConfigLayer;
        this.envConfigLayer = envConfigLayer;
        this.systemConfigLayer = systemConfigLayer;
        this.resolver = resolver;
        this.envProvider = envProvider;
    }

    @GetMapping("/{configKey}")
    public Map<String, Object> debug(@PathVariable String configKey) {
        Long tenantId = resolveTenantId();
        String env = envProvider.getEnv();
        ConfigContext context = new ConfigContext(env, tenantId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("env", env);
        result.put("tenantId", tenantId);
        result.put("resolved", resolver.resolve(configKey, context).orElse(null));
        result.put("layers", layerValues(configKey, context));
        return result;
    }

    private Map<String, Object> layerValues(String configKey, ConfigContext context) {
        Map<String, Object> layers = new LinkedHashMap<>();
        layers.put("tenantEnv", tenantEnvConfigLayer.get(configKey, context).orElse(null));
        layers.put("tenantGlobal", tenantGlobalConfigLayer.get(configKey, context).orElse(null));
        layers.put("env", envConfigLayer.get(configKey, context).orElse(null));
        layers.put("system", systemConfigLayer.get(configKey, context).orElse(null));
        return layers;
    }

    private Long resolveTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
