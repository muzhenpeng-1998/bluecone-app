package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 租户素材视图。
 */
@Value
@Builder
public class TenantMediaView {
    Long id;
    String mediaType;
    String url;
    String description;
    LocalDateTime createdAt;
}
