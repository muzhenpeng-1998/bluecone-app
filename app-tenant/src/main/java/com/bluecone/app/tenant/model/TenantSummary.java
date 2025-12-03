package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 租户精简视图，供列表/下拉使用。
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
