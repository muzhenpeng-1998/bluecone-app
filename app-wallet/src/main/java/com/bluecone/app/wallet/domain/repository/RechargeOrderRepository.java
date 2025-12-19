package com.bluecone.app.wallet.domain.repository;

import com.bluecone.app.wallet.domain.model.RechargeOrder;

import java.util.Optional;

/**
 * 充值单仓储接口
 * 
 * @author bluecone
 * @since 2025-12-19
 */
public interface RechargeOrderRepository {
    
    /**
     * 保存充值单
     */
    RechargeOrder save(RechargeOrder rechargeOrder);
    
    /**
     * 根据ID查询充值单
     */
    Optional<RechargeOrder> findById(Long tenantId, Long id);
    
    /**
     * 根据充值单号查询
     */
    Optional<RechargeOrder> findByRechargeNo(Long tenantId, String rechargeNo);
    
    /**
     * 根据幂等键查询
     */
    Optional<RechargeOrder> findByIdempotencyKey(Long tenantId, String idempotencyKey);
    
    /**
     * 根据渠道交易号查询（用于支付回调幂等）
     */
    Optional<RechargeOrder> findByChannelTradeNo(Long tenantId, String channelTradeNo);
    
    /**
     * 乐观锁更新充值单
     * 
     * @return 更新行数（0表示版本冲突）
     */
    int updateWithVersion(RechargeOrder rechargeOrder);
}
