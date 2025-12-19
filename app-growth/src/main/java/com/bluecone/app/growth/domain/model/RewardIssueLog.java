package com.bluecone.app.growth.domain.model;

import com.bluecone.app.growth.api.enums.IssueStatus;
import com.bluecone.app.growth.api.enums.RewardType;
import com.bluecone.app.growth.api.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 奖励发放日志领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardIssueLog {
    
    /**
     * 发放记录ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 活动编码
     */
    private String campaignCode;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 归因记录ID
     */
    private Long attributionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户角色
     */
    private UserRole userRole;
    
    /**
     * 奖励类型
     */
    private RewardType rewardType;
    
    /**
     * 奖励值（JSON格式）
     */
    private String rewardValue;
    
    /**
     * 发放状态
     */
    private IssueStatus issueStatus;
    
    /**
     * 发放结果ID（券ID/账本流水ID）
     */
    private String resultId;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 触发订单ID
     */
    private Long triggerOrderId;
    
    /**
     * 发放成功时间
     */
    private LocalDateTime issuedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 标记为成功
     */
    public void markSuccess(String resultId) {
        this.issueStatus = IssueStatus.SUCCESS;
        this.resultId = resultId;
        this.issuedAt = LocalDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
    }
    
    /**
     * 标记为失败
     */
    public void markFailed(String errorCode, String errorMessage) {
        this.issueStatus = IssueStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return issueStatus == IssueStatus.SUCCESS;
    }
    
    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return issueStatus == IssueStatus.FAILED;
    }
}
