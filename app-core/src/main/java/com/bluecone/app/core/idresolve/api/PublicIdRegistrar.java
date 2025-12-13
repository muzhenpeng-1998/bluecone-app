package com.bluecone.app.core.idresolve.api;

import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;

/**
 * 公共 ID 映射注册器，在创建业务主表时同步写入映射表。
 */
public interface PublicIdRegistrar {

    /**
     * 注册一条 publicId -> internalId 映射，要求与业务主表写入处于同一事务。
     *
     * @param tenantId  租户 ID
     * @param type      资源类型
     * @param publicId  对外 ID
     * @param internalId 内部 ULID128
     */
    void register(long tenantId, ResourceType type, String publicId, Ulid128 internalId);
}

