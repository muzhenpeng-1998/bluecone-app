package com.bluecone.app.growth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 绑定邀请码请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindInviteRequest {
    
    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
    
    /**
     * 活动编码（可选，如果不传则从邀请码查询）
     */
    private String campaignCode;
}
