package com.bluecone.app.campaign.domain.repository;

import com.bluecone.app.campaign.domain.model.ExecutionLog;

import java.util.List;
import java.util.Optional;

/**
 * 活动执行日志仓储接口
 */
public interface ExecutionLogRepository {
    
    /**
     * 保存执行日志
     */
    void save(ExecutionLog log);
    
    /**
     * 更新执行日志
     */
    void update(ExecutionLog log);
    
    /**
     * 根据幂等键查询
     */
    Optional<ExecutionLog> findByIdempotencyKey(Long tenantId, String idempotencyKey);
    
    /**
     * 查询用户在某个活动的执行次数
     */
    int countUserExecutions(Long tenantId, Long campaignId, Long userId);
    
    /**
     * 查询活动执行日志列表
     */
    List<ExecutionLog> findByConditions(Long tenantId, Long campaignId, Long userId, Integer limit);
}
