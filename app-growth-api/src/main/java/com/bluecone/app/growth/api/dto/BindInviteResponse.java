package com.bluecone.app.growth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 绑定邀请码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindInviteResponse {
    
    /**
     * 归因ID
     */
    private Long attributionId;
    
    /**
     * 是否绑定成功
     */
    private Boolean success;
    
    /**
     * 活动编码
     */
    private String campaignCode;
    
    /**
     * 邀请码
     */
    private String inviteCode;
    
    /**
     * 提示信息
     */
    private String message;
}
