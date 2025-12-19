package com.bluecone.app.growth.domain.repository;

import com.bluecone.app.growth.domain.model.Attribution;

import java.util.Optional;

/**
 * 归因关系仓储接口
 */
public interface AttributionRepository {
    
    /**
     * 保存归因关系
     */
    void save(Attribution attribution);
    
    /**
     * 更新归因关系
     */
    void update(Attribution attribution);
    
    /**
     * 根据租户ID、活动编码、被邀请人ID查询
     */
    Optional<Attribution> findByInvitee(Long tenantId, String campaignCode, Long inviteeUserId);
    
    /**
     * 根据ID查询
     */
    Optional<Attribution> findById(Long id);
    
    /**
     * 统计用户在租户下的已支付订单数
     * 用于判断是否首单
     */
    int countPaidOrdersByUser(Long tenantId, Long userId);
}
