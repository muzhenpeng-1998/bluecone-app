package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * 租户详情聚合，聚合租户主体、认证、套餐、平台账号等。
 */
@Value
@Builder
public class TenantDetail {
    Long tenantId;
    String tenantCode;
    String tenantName;
    Integer status;
    String contactPerson;
    String contactPhone;
    String contactEmail;
    String remark;
    TenantVerificationInfo verificationInfo;
    TenantPlanInfo planInfo;
    List<TenantPlatformAccountView> platformAccounts;
    List<TenantMediaView> mediaList;
    Map<String, String> settings;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
