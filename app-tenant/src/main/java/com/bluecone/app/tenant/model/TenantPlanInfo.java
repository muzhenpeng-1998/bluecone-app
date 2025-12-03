package com.bluecone.app.tenant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 套餐信息视图。
 * - 来自 settings 或 billing 与 plan 表组合
 * - 便于在列表/详情中统一展示套餐名、价格、到期时间
 */
@Value
@Builder
public class TenantPlanInfo {
    Long planId;
    String planName;
    BigDecimal price;
    String features;
    Byte status;
    LocalDateTime expireAt;
}
