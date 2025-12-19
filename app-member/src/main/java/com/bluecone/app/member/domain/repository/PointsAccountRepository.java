package com.bluecone.app.member.domain.repository;

import com.bluecone.app.member.domain.model.PointsAccount;

import java.util.Optional;

/**
 * 积分账户仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface PointsAccountRepository {
    
    /**
     * 根据会员ID查询积分账户
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 积分账户
     */
    Optional<PointsAccount> findByMemberId(Long tenantId, Long memberId);
    
    /**
     * 保存积分账户
     * 
     * @param account 积分账户
     */
    void save(PointsAccount account);
    
    /**
     * 更新积分账户（使用乐观锁）
     * 
     * @param account 积分账户
     * @return 是否更新成功（乐观锁失败返回 false）
     */
    boolean updateWithVersion(PointsAccount account);
}
