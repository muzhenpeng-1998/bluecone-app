package com.bluecone.app.campaign.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 活动规则 DTO（JSON 序列化）
 * 不同活动类型对应不同的规则字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRulesDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ========== 通用规则 ==========
    
    /**
     * 最低订单/充值金额门槛
     */
    private BigDecimal minAmount;
    
    /**
     * 是否限首单（首次订单/充值）
     */
    @Builder.Default
    private Boolean firstOrderOnly = false;
    
    /**
     * 每用户参与次数限制（null 表示不限）
     */
    private Integer perUserLimit;
    
    // ========== ORDER_DISCOUNT 专用规则 ==========
    
    /**
     * 满减金额
     */
    private BigDecimal discountAmount;
    
    /**
     * 折扣率（如 0.85 表示 85 折）
     */
    private BigDecimal discountRate;
    
    /**
     * 最高优惠封顶金额
     */
    private BigDecimal maxDiscountAmount;
    
    // ========== ORDER_REBATE_COUPON 专用规则 ==========
    
    /**
     * 返券模板ID列表（逗号分隔）
     */
    private String couponTemplateIds;
    
    /**
     * 每个模板发放数量（默认1张）
     */
    @Builder.Default
    private Integer couponQuantity = 1;
    
    // ========== RECHARGE_BONUS 专用规则 ==========
    
    /**
     * 赠送金额
     */
    private BigDecimal bonusAmount;
    
    /**
     * 赠送比例（如 0.1 表示充值额外赠送 10%）
     */
    private BigDecimal bonusRate;
    
    /**
     * 最高赠送封顶金额
     */
    private BigDecimal maxBonusAmount;
}
