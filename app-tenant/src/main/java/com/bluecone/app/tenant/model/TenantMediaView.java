package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 租户素材视图。
 * - 列表展示租户的头像/证照等媒体资源
 * - 预留后续媒体审核、CDN 加速等扩展
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
