package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantProfileUpdateRequest {

    @NotNull
    private Byte tenantType;

    @NotBlank
    private String businessName;

    private String businessLicenseNo;

    private String businessLicenseUrl;

    private String legalPersonName;

    private String legalPersonIdNo;

    private String address;

    private Byte verificationStatus;

    private String verificationReason;

    private Long operatorId;
}
