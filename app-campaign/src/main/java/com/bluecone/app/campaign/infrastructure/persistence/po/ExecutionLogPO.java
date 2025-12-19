package com.bluecone.app.campaign.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动执行日志持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bc_campaign_execution_log")
public class ExecutionLogPO {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.INPUT)
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
    private String campaignType;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 业务单ID
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
    private String executionStatus;
    
    /**
     * 奖励金额
     */
    private BigDecimal rewardAmount;
    
    /**
     * 奖励结果ID
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
}
