package com.bluecone.app.growth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 奖励发放日志表PO
 */
@Data
@TableName("bc_growth_reward_issue_log")
public class RewardIssueLogPO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private String campaignCode;
    
    private String idempotencyKey;
    
    private Long attributionId;
    
    private Long userId;
    
    private String userRole;
    
    private String rewardType;
    
    private String rewardValue;
    
    private String issueStatus;
    
    private String resultId;
    
    private String errorCode;
    
    private String errorMessage;
    
    private Long triggerOrderId;
    
    private LocalDateTime issuedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
