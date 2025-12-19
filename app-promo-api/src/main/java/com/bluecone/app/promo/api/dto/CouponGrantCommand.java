package com.bluecone.app.promo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 券发放命令（单个用户）
 * 用于系统内部模块间调用，如营销活动发券
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantCommand {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 券模板ID
     */
    private Long templateId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 幂等键（确保同一请求不会重复发券）
     */
    private String idempotencyKey;

    /**
     * 发放原因/备注
     */
    private String grantReason;

    /**
     * 业务来源（可选）
     */
    private String source;
}
