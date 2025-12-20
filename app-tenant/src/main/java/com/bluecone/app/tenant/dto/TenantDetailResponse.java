package com.bluecone.app.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantDetailResponse {
    Long tenantId;
    String publicId;
    String tenantCode;
    String tenantName;
    Integer status;
    String contactPerson;
    String contactPhone;
    String contactEmail;
    String remark;
    TenantVerificationInfoResponse verification;
    TenantPlanInfoResponse plan;
    List<PlatformAccountResponse> platformAccounts;
    List<TenantMediaResponse> mediaList;
    Map<String, String> settings;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Value
    @Builder
    public static class TenantVerificationInfoResponse {
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

    @Value
    @Builder
    public static class TenantPlanInfoResponse {
        Long planId;
        String planName;
        BigDecimal price;
        String features;
        Byte status;
        LocalDateTime expireAt;
    }

    @Value
    @Builder
    public static class PlatformAccountResponse {
        Long id;
        String platformType;
        String platformAccountId;
        String accountName;
        Byte status;
        LocalDateTime expireAt;
    }

    @Value
    @Builder
    public static class TenantMediaResponse {
        Long id;
        String mediaType;
        String url;
        String description;
        LocalDateTime createdAt;
    }
}
