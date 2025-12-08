package com.bluecone.app.infra.configcenter.layer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bluecone.app.core.config.ConfigContext;
import com.bluecone.app.infra.configcenter.mapper.ConfigPropertyMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfigContextTest {

    @Test
    // 验证配置解析的优先级：租户+环境 > 租户全局 > 环境 > 系统默认
    void resolvesWithExpectedPrecedence() {
        ConfigContext context = new ConfigContext("prod", 42L);

        TenantEnvConfigLayer tenantEnv = new StubTenantEnvLayer(Map.of(
                key(42L, "prod", "order.timeout"), "tenant-prod",
                key(42L, "test", "order.timeout"), "tenant-test"
        ));
        TenantGlobalConfigLayer tenantGlobal = new StubTenantGlobalLayer(Map.of(
                key(7L, null, "order.timeout"), "tenant-global"
        ));
        EnvConfigLayer envLayer = new StubEnvLayer(Map.of(
                key(null, "prod", "order.timeout"), "env-prod"
        ));
        SystemConfigLayer systemLayer = new StubSystemLayer(Map.of(
                key(null, null, "order.timeout"), "system-default"
        ));

        LayeredConfigResolver resolver = new LayeredConfigResolver(
                tenantEnv,
                tenantGlobal,
                envLayer,
                systemLayer);

        assertThat(resolver.resolve("order.timeout", context))
                .contains("tenant-prod");

        ConfigContext missingTenantEnv = new ConfigContext("prod", 7L);
        assertThat(resolver.resolve("order.timeout", missingTenantEnv))
                .contains("tenant-global");

        ConfigContext missingTenant = new ConfigContext("prod", null);
        assertThat(resolver.resolve("order.timeout", missingTenant))
                .contains("env-prod");

        ConfigContext missingEverything = new ConfigContext("stage", null);
        assertThat(resolver.resolve("order.timeout", missingEverything))
                .contains("system-default");

        ConfigContext unknownKey = new ConfigContext("stage", 99L);
        assertThat(resolver.resolve("order.max", unknownKey)).isEmpty();
    }

    private static String key(Long tenantId, String env, String configKey) {
        return (tenantId != null ? tenantId : "global") + "|" + (env != null ? env : "global") + "|" + configKey;
    }

    private static class StubTenantEnvLayer extends TenantEnvConfigLayer {
        private final Map<String, String> store;

        StubTenantEnvLayer(Map<String, String> store) {
            super(mock(ConfigPropertyMapper.class));
            this.store = store;
        }

        @Override
        public Optional<String> get(String configKey, ConfigContext context) {
            if (context.tenantId() == null || context.env() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(store.get(key(context.tenantId(), context.env(), configKey)));
        }
    }

    private static class StubTenantGlobalLayer extends TenantGlobalConfigLayer {
        private final Map<String, String> store;

        StubTenantGlobalLayer(Map<String, String> store) {
            super(mock(ConfigPropertyMapper.class));
            this.store = store;
        }

        @Override
        public Optional<String> get(String configKey, ConfigContext context) {
            if (context.tenantId() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(store.get(key(context.tenantId(), null, configKey)));
        }
    }

    private static class StubEnvLayer extends EnvConfigLayer {
        private final Map<String, String> store;

        StubEnvLayer(Map<String, String> store) {
            super(mock(ConfigPropertyMapper.class));
            this.store = store;
        }

        @Override
        public Optional<String> get(String configKey, ConfigContext context) {
            if (context.env() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(store.get(key(null, context.env(), configKey)));
        }
    }

    private static class StubSystemLayer extends SystemConfigLayer {
        private final Map<String, String> store;

        StubSystemLayer(Map<String, String> store) {
            super(mock(ConfigPropertyMapper.class));
            this.store = store;
        }

        @Override
        public Optional<String> get(String configKey, ConfigContext context) {
            return Optional.ofNullable(store.get(key(null, null, configKey)));
        }
    }
}
