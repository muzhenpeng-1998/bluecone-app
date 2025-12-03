package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TenantPlanChangeRequest {

    @NotNull
    private Long planId;

    private BigDecimal payAmount;

    private String payMethod;

    private LocalDateTime expireAt;

    private Long operatorId;
}
