package com.bluecone.app.tenant.model.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐变更命令。
 */
public record ChangeTenantPlanCommand(
        Long tenantId,
        Long planId,
        BigDecimal payAmount,
        String payMethod,
        LocalDateTime expireAt,
        Long operatorId) {
}
