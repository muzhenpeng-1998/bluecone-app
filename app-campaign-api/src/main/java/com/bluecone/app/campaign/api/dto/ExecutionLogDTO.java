package com.bluecone.app.campaign.api.dto;

import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动执行日志 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 活动ID
     */
    private Long campaignId;
    
    /**
     * 活动编码
     */
    private String campaignCode;
    
    /**
     * 活动类型
     */
    private CampaignType campaignType;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 业务单ID（订单ID/充值ID）
     */
    private Long bizOrderId;
    
    /**
     * 业务单号
     */
    private String bizOrderNo;
    
    /**
     * 业务金额
     */
    private BigDecimal bizAmount;
    
    /**
     * 执行状态
     */
    private ExecutionStatus executionStatus;
    
    /**
     * 奖励金额
     */
    private BigDecimal rewardAmount;
    
    /**
     * 奖励结果ID（券ID/账本流水ID）
     */
    private String rewardResultId;
    
    /**
     * 失败原因
     */
    private String failureReason;
    
    /**
     * 执行时间
     */
    private LocalDateTime executedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
