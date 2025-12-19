package com.bluecone.app.campaign.domain.model;

import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动执行日志领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLog {
    
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
     * 幂等键（全局唯一）
     * 格式：{tenantId}:{campaignType}:{bizOrderId}:{userId}
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
     * 奖励结果ID（券ID/账本流水ID，多个用逗号分隔）
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
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 标记执行成功
     */
    public void markSuccess(BigDecimal rewardAmount, String rewardResultId) {
        this.executionStatus = ExecutionStatus.SUCCESS;
        this.rewardAmount = rewardAmount;
        this.rewardResultId = rewardResultId;
        this.executedAt = LocalDateTime.now();
    }
    
    /**
     * 标记执行失败
     */
    public void markFailed(String failureReason) {
        this.executionStatus = ExecutionStatus.FAILED;
        this.failureReason = failureReason;
        this.executedAt = LocalDateTime.now();
    }
    
    /**
     * 标记跳过（条件不满足）
     */
    public void markSkipped(String reason) {
        this.executionStatus = ExecutionStatus.SKIPPED;
        this.failureReason = reason;
        this.executedAt = LocalDateTime.now();
    }
}
