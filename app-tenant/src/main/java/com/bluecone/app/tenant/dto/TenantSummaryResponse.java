package com.bluecone.app.dto.tenant;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantSummaryResponse {
    Long tenantId;
    String publicId;
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
