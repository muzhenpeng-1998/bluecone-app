package com.bluecone.app.resource.api.dto;

import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;

/**
 * 资源解绑命令，移除某个业务对象与目标资源的关联关系。
 *
 * @param ownerType        业务对象类型
 * @param ownerId          业务对象 ID
 * @param purpose          资源用途
 * @param resourceObjectId 要解绑的资源对象 ID
 * @param sortOrder        可选排序，用于精确匹配特定绑定
 * @param isMain           指定是否解绑主资源
 */
public record UnbindResourceCommand(ResourceOwnerType ownerType,
                                    Long ownerId,
                                    ResourcePurpose purpose,
                                    String resourceObjectId,
                                    Integer sortOrder,
                                    Boolean isMain) {
}
