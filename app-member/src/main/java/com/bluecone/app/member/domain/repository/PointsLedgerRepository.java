package com.bluecone.app.member.domain.repository;

import com.bluecone.app.member.domain.model.PointsLedger;

import java.util.Optional;

/**
 * 积分流水仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface PointsLedgerRepository {
    
    /**
     * 根据幂等键查询流水记录
     * 用于判断是否已处理过该操作
     * 
     * @param tenantId 租户ID
     * @param idempotencyKey 幂等键
     * @return 流水记录
     */
    Optional<PointsLedger> findByIdempotencyKey(Long tenantId, String idempotencyKey);
    
    /**
     * 保存流水记录
     * 如果幂等键冲突（唯一约束），则抛出异常或返回 false
     * 
     * @param ledger 流水记录
     * @return 是否保存成功
     */
    boolean save(PointsLedger ledger);
}
