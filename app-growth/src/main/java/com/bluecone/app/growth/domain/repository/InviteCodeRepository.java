package com.bluecone.app.growth.domain.repository;

import com.bluecone.app.growth.domain.model.InviteCode;

import java.util.Optional;

/**
 * 邀请码仓储接口
 */
public interface InviteCodeRepository {
    
    /**
     * 保存邀请码
     */
    void save(InviteCode inviteCode);
    
    /**
     * 更新邀请码
     */
    void update(InviteCode inviteCode);
    
    /**
     * 根据邀请码查询
     */
    Optional<InviteCode> findByInviteCode(String inviteCode);
    
    /**
     * 根据租户ID、活动编码、邀请人ID查询
     */
    Optional<InviteCode> findByInviter(Long tenantId, String campaignCode, Long inviterUserId);
    
    /**
     * 增加邀请计数
     */
    void incrementInvitesCount(Long id);
    
    /**
     * 增加成功邀请计数
     */
    void incrementSuccessfulInvitesCount(Long id);
}
