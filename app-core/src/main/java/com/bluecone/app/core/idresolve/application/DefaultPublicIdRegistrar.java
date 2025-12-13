package com.bluecone.app.core.idresolve.application;

import java.util.Objects;

import com.bluecone.app.core.idresolve.api.PublicIdRegistrar;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;

/**
 * 默认公共 ID 注册器实现，直接写入 bc_public_id_map 映射表。
 */
public class DefaultPublicIdRegistrar implements PublicIdRegistrar {

    private final PublicIdMapRepository repository;

    public DefaultPublicIdRegistrar(PublicIdMapRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void register(long tenantId, ResourceType type, String publicId, Ulid128 internalId) {
        if (tenantId <= 0 || type == null || publicId == null || publicId.isBlank() || internalId == null) {
            return;
        }
        repository.insertMapping(tenantId, type.name(), publicId, internalId);
    }
}

