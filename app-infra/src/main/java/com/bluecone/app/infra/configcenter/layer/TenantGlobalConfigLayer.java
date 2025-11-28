package com.bluecone.app.infra.configcenter.layer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bluecone.app.core.config.ConfigContext;
import com.bluecone.app.core.config.ConfigScope;
import com.bluecone.app.infra.configcenter.entity.ConfigPropertyEntity;
import com.bluecone.app.infra.configcenter.mapper.ConfigPropertyMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tenant global configuration layer (no env bound).
 */
@Component
public class TenantGlobalConfigLayer implements ConfigLayer {

    private final ConfigPropertyMapper mapper;

    public TenantGlobalConfigLayer(ConfigPropertyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<String> get(String configKey, ConfigContext context) {
        if (context.tenantId() == null) {
            return Optional.empty();
        }
        QueryWrapper<ConfigPropertyEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(ConfigPropertyEntity::getConfigKey, configKey)
                .eq(ConfigPropertyEntity::getScope, ConfigScope.TENANT.name())
                .isNull(ConfigPropertyEntity::getEnv)
                .eq(ConfigPropertyEntity::getTenantId, context.tenantId())
                .eq(ConfigPropertyEntity::getEnabled, Boolean.TRUE)
                .last("limit 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(ConfigPropertyEntity::getConfigValue);
    }
}
