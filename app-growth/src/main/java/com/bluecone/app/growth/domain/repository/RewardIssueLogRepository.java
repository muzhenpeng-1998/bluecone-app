package com.bluecone.app.growth.domain.repository;

import com.bluecone.app.growth.domain.model.RewardIssueLog;

import java.util.Optional;

/**
 * 奖励发放日志仓储接口
 */
public interface RewardIssueLogRepository {
    
    /**
     * 保存发放日志
     */
    void save(RewardIssueLog log);
    
    /**
     * 更新发放日志
     */
    void update(RewardIssueLog log);
    
    /**
     * 根据幂等键查询
     */
    Optional<RewardIssueLog> findByIdempotencyKey(Long tenantId, String idempotencyKey);
    
    /**
     * 根据归因ID和用户角色查询
     */
    Optional<RewardIssueLog> findByAttributionAndRole(Long tenantId, Long attributionId, String userRole);
}
