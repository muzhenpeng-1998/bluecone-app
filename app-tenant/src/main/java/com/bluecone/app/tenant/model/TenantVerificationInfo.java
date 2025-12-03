package com.bluecone.app.tenant.model;

import lombok.Builder;
import lombok.Value;

/**
 * 与认证/主体资料相关的视图。
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
