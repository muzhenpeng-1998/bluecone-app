package com.bluecone.app.core.idresolve.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bluecone.app.id.core.Ulid128;

/**
 * 公共 ID 映射表访问接口，对应表 bc_public_id_map。
 */
public interface PublicIdMapRepository {

    /**
     * 按 tenant + resourceType + publicId 查找内部 ULID128。
     */
    Optional<Ulid128> findInternalId(long tenantId, String resourceType, String publicId);

    /**
     * 批量按 publicId 查询，返回 Map 的 key 为 publicId 字符串。
     */
    Map<String, Ulid128> findInternalIds(long tenantId, String resourceType, List<String> publicIds);

    /**
     * 插入一条映射记录。
     */
    void insertMapping(long tenantId, String resourceType, String publicId, Ulid128 internalId);
}

