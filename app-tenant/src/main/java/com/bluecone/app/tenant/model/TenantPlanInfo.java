package com.bluecone.app.tenant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 套餐信息视图。
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
