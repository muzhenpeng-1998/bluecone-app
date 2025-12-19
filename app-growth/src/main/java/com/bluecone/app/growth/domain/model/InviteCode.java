package com.bluecone.app.growth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邀请码领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCode {
    
    /**
     * 邀请码记录ID
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
     * 邀请人数
     */
    private Integer invitesCount;
    
    /**
     * 成功邀请人数（完成首单）
     */
    private Integer successfulInvitesCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 增加邀请计数
     */
    public void incrementInvitesCount() {
        if (this.invitesCount == null) {
            this.invitesCount = 0;
        }
        this.invitesCount++;
    }
    
    /**
     * 增加成功邀请计数
     */
    public void incrementSuccessfulInvitesCount() {
        if (this.successfulInvitesCount == null) {
            this.successfulInvitesCount = 0;
        }
        this.successfulInvitesCount++;
    }
}
