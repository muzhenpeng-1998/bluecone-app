package com.bluecone.app.core.idresolve.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;

/**
 * 公共 ID 业务回退查找接口，用于迁移期在映射表 miss 时回退到业务主表查询。
 */
public interface PublicIdFallbackLookup {

    /**
     * 是否支持给定资源类型。
     */
    boolean supports(ResourceType type);

    /**
     * 回退查找单个 publicId。
     */
    Optional<Ulid128> findInternalId(long tenantId, ResourceType type, String publicId);

    /**
     * 回退批量查找 publicId。
     */
    Map<String, Ulid128> findInternalIds(long tenantId, ResourceType type, List<String> publicIds);
}

