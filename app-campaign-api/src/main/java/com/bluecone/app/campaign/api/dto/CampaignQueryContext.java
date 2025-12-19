package com.bluecone.app.campaign.api.dto;

import com.bluecone.app.campaign.api.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动查询上下文
 * 用于在计价阶段或事件消费时查询可用活动
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignQueryContext implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 租户ID（必填）
     */
    private Long tenantId;
    
    /**
     * 活动类型（必填）
     */
    private CampaignType campaignType;
    
    /**
     * 门店ID（可选）
     */
    private Long storeId;
    
    /**
     * 渠道（可选）
     */
    private String channel;
    
    /**
     * 用户ID（用于检查首单和参与次数）
     */
    private Long userId;
    
    /**
     * 订单/充值金额（用于匹配门槛）
     */
    private BigDecimal amount;
    
    /**
     * 查询时间（默认当前时间）
     */
    @Builder.Default
    private LocalDateTime queryTime = LocalDateTime.now();
}
