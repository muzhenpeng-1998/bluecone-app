package com.bluecone.app.api.config;

import com.bluecone.app.infra.configcenter.entity.ConfigPropertyEntity;
import com.bluecone.app.infra.configcenter.mapper.ConfigPropertyMapper;
import com.bluecone.app.infra.configcenter.snapshot.ConfigSnapshotManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Admin endpoints for managing ConfigCenter properties.
 * Note: production must protect these endpoints with authentication/authorization.
 */
@RestController
@RequestMapping("/api/admin/config")
public class ConfigAdminController {

    private final ConfigPropertyMapper mapper;
    private final ConfigSnapshotManager snapshotManager;

    public ConfigAdminController(ConfigPropertyMapper mapper, ConfigSnapshotManager snapshotManager) {
        this.mapper = mapper;
        this.snapshotManager = snapshotManager;
    }

    @GetMapping
    public List<ConfigPropertyEntity> list() {
        return mapper.selectList(null);
    }

    @PostMapping
    public ConfigPropertyEntity create(@RequestBody ConfigPropertyRequest request) {
        ConfigPropertyEntity entity = toEntity(new ConfigPropertyEntity(), request);
        mapper.insert(entity);
        return entity;
    }

    @PutMapping("/{id}")
    public ConfigPropertyEntity update(@PathVariable Long id, @RequestBody ConfigPropertyRequest request) {
        ConfigPropertyEntity entity = mapper.selectById(id);
        Objects.requireNonNull(entity, "config not found");
        toEntity(entity, request);
        mapper.updateById(entity);
        return entity;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mapper.deleteById(id);
    }

    @PostMapping("/refresh")
    public String refresh() {
        snapshotManager.refreshAll();
        return "OK";
    }

    private ConfigPropertyEntity toEntity(ConfigPropertyEntity entity, ConfigPropertyRequest request) {
        entity.setConfigKey(request.configKey());
        entity.setConfigValue(request.configValue());
        entity.setValueType(request.valueType() == null ? "STRING" : request.valueType());
        entity.setScope(request.scope());
        entity.setEnv(request.env());
        entity.setTenantId(request.tenantId());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setRemark(request.remark());
        return entity;
    }

    /**
     * Simple request payload for config mutations.
     */
    public record ConfigPropertyRequest(String configKey,
                                        String configValue,
                                        String valueType,
                                        String scope,
                                        String env,
                                        Long tenantId,
                                        Boolean enabled,
                                        String remark) {
    }
}
