package com.bluecone.app.growth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeResponse {
    
    /**
     * 邀请码
     */
    private String inviteCode;
    
    /**
     * 活动编码
     */
    private String campaignCode;
    
    /**
     * 邀请链接（完整URL）
     */
    private String inviteLink;
    
    /**
     * 已邀请人数
     */
    private Integer invitesCount;
    
    /**
     * 成功邀请人数（完成首单）
     */
    private Integer successfulInvitesCount;
}
