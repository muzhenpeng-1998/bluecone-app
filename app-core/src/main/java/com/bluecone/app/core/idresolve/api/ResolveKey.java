package com.bluecone.app.core.idresolve.api;

import com.bluecone.app.id.api.ResourceType;

/**
 * 公共 ID 解析键，聚合租户 + 资源类型 + publicId。
 *
 * @param tenantId 当前租户 ID
 * @param type     资源类型（TENANT/STORE/ORDER/USER/PRODUCT 等）
 * @param publicId 对外 ID（publicId）
 */
public record ResolveKey(long tenantId, ResourceType type, String publicId) {
}

