package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantBasicInfoUpdateRequest {

    @NotBlank
    @Size(max = 64)
    private String tenantName;

    @Size(max = 32)
    private String contactPerson;

    @Size(max = 32)
    private String contactPhone;

    @Email
    private String contactEmail;

    @Size(max = 128)
    private String remark;

    @NotNull
    private Integer status;

    private Long operatorId;
}
