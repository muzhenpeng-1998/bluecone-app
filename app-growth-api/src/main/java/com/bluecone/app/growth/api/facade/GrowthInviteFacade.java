package com.bluecone.app.growth.api.facade;

import com.bluecone.app.growth.api.dto.BindInviteRequest;
import com.bluecone.app.growth.api.dto.BindInviteResponse;
import com.bluecone.app.growth.api.dto.InviteCodeResponse;

/**
 * 增长引擎-邀请门面
 * 供业务模块调用的邀请相关接口
 */
public interface GrowthInviteFacade {
    
    /**
     * 生成或获取邀请码
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID（邀请人）
     * @param campaignCode 活动编码
     * @return 邀请码信息
     */
    InviteCodeResponse getOrCreateInviteCode(Long tenantId, Long userId, String campaignCode);
    
    /**
     * 绑定邀请码（新客绑定归因）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID（被邀请人）
     * @param request 绑定请求
     * @return 绑定结果
     */
    BindInviteResponse bindInviteCode(Long tenantId, Long userId, BindInviteRequest request);
}
