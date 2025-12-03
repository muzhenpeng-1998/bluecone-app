package com.bluecone.app.tenant.model.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐变更命令。
 * - planId 对应 tenant_plan 的主键
 * - payAmount/payMethod 描述本次付费记录，支持为空（赠送/试用）
 * - expireAt 写入 settings 及 billing，用于后续校验
 */
public record ChangeTenantPlanCommand(
        Long tenantId,
        Long planId,
        BigDecimal payAmount,
        String payMethod,
        LocalDateTime expireAt,
        Long operatorId) {
}
