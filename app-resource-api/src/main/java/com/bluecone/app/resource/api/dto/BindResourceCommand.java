package com.bluecone.app.resource.api.dto;

import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;

/**
 * 资源绑定命令，驱动已有资源与业务层建立关联。
 *
 * @param ownerType        业务对象类型
 * @param ownerId          业务对象 ID
 * @param purpose          资源用途
 * @param resourceObjectId 要绑定的资源对象 ID
 * @param sortOrder        排序权重，越小优先级越高
 * @param isMain           是否作为主资源
 */
public record BindResourceCommand(ResourceOwnerType ownerType,
                                  Long ownerId,
                                  ResourcePurpose purpose,
                                  String resourceObjectId,
                                  Integer sortOrder,
                                  Boolean isMain) {
}
