package com.bluecone.app.growth.domain.model;

import com.bluecone.app.growth.api.enums.AttributionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 归因关系领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attribution {
    
    /**
     * 归因ID
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
     * 邀请码
     */
    private String inviteCode;
    
    /**
     * 邀请人用户ID
     */
    private Long inviterUserId;
    
    /**
     * 被邀请人用户ID
     */
    private Long inviteeUserId;
    
    /**
     * 归因状态
     */
    private AttributionStatus status;
    
    /**
     * 绑定时间
     */
    private LocalDateTime boundAt;
    
    /**
     * 确认时间（首单完成时间）
     */
    private LocalDateTime confirmedAt;
    
    /**
     * 首单订单ID
     */
    private Long firstOrderId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 确认归因（首单完成）
     */
    public void confirm(Long orderId) {
        this.status = AttributionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.firstOrderId = orderId;
    }
    
    /**
     * 使归因失效
     */
    public void invalidate() {
        this.status = AttributionStatus.INVALID;
    }
    
    /**
     * 检查是否可以触发奖励
     */
    public boolean canTriggerReward() {
        return status == AttributionStatus.PENDING;
    }
}
