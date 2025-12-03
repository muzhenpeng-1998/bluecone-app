package com.bluecone.app.tenant.model;

import lombok.Builder;
import lombok.Value;

/**
 * 与认证/主体资料相关的视图。
 * - 展示认证状态与驳回原因，方便前端展示与重提
 * - 仅包含主体相关字段，不含业务无关数据
 */
@Value
@Builder
public class TenantVerificationInfo {
    Byte tenantType;
    String businessName;
    String businessLicenseNo;
    String businessLicenseUrl;
    String legalPersonName;
    String legalPersonIdNo;
    String address;
    Byte verificationStatus;
    String verificationReason;
}
