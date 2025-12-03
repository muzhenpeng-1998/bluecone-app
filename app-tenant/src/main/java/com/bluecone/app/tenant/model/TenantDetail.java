package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * 租户详情聚合视图。
 * - 面向上层 API 的只读模型，不直接暴露 Entity
 * - 聚合 tenant 主表 + profile + plan + platformAccounts + media + settings
 * - 便于一次查询返回完整信息，减少 Controller/网关拼装
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
