package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对应 bc_store_resource 的领域模型，描述门店内部资源（桌台、房间、场馆资源等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResourceModel {

    private String resourceType;

    private String code;

    private String name;

    private String area;

    private String status;

    private String metadataJson;
}
