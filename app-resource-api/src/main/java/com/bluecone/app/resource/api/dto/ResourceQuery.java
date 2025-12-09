package com.bluecone.app.resource.api.dto;

import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;

/**
 * 资源查询条件，支持分页与排序信息。
 *
 * @param ownerType  查询的归属类型
 * @param ownerId    查询的业务实体 ID
 * @param purpose    查询的资源用途
 * @param pageNo     页码，从 1 开始；为空时默认为 1
 * @param pageSize   每页条数，空值表示不分页
 * @param sortBy     可选排序字段
 * @param descending 是否倒序
 */
public record ResourceQuery(ResourceOwnerType ownerType,
                            Long ownerId,
                            ResourcePurpose purpose,
                            Integer pageNo,
                            Integer pageSize,
                            String sortBy,
                            Boolean descending) {
}
