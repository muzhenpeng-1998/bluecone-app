package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 租户精简视图。
 * - 主要用于列表/下拉展示，包含套餐信息的核心字段
 * - 避免在列表场景加载冗余大字段（媒体、平台账号等）
 */
@Value
@Builder
public class TenantSummary {
    Long tenantId;
    String tenantCode;
    String tenantName;
    Integer status;
    String contactPerson;
    String contactPhone;
    Long planId;
    String planName;
    LocalDateTime planExpireAt;
    LocalDateTime createdAt;
}
