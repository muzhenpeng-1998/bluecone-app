package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TenantCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String tenantName;

    @NotBlank
    @Size(max = 32)
    private String contactPerson;

    @NotBlank
    @Size(max = 32)
    private String contactPhone;

    @Email
    private String contactEmail;

    @Size(max = 128)
    private String remark;

    @NotNull
    private Byte tenantType;

    @NotBlank
    private String businessName;

    private String businessLicenseNo;

    private String businessLicenseUrl;

    private String legalPersonName;

    private String legalPersonIdNo;

    private String address;

    private Long initialPlanId;

    private LocalDateTime planExpireAt;

    /**
     * 预留给审计使用，由安全层填充。
     */
    private Long operatorId;
}
