package com.bluecone.app.wallet.domain.service;

import com.bluecone.app.wallet.domain.model.RechargeOrder;

import java.time.LocalDateTime;

/**
 * 充值单领域服务接口
 * 
 * @author bluecone
 * @since 2025-12-19
 */
public interface RechargeOrderDomainService {
    
    /**
     * 创建充值单
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param accountId 账户ID
     * @param rechargeAmountInCents 充值金额（分）
     * @param bonusAmountInCents 赠送金额（分）
     * @param idempotencyKey 幂等键
     * @return 充值单
     */
    RechargeOrder createRechargeOrder(Long tenantId, Long userId, Long accountId,
                                     Long rechargeAmountInCents, Long bonusAmountInCents,
                                     String idempotencyKey);
    
    /**
     * 标记充值单为支付中
     * 
     * @param rechargeNo 充值单号
     * @param payOrderId 支付单ID
     * @param payChannel 支付渠道
     */
    void markAsPaying(Long tenantId, String rechargeNo, Long payOrderId, String payChannel);
    
    /**
     * 标记充值单为已支付（支付回调触发）
     * 
     * @param tenantId 租户ID
     * @param rechargeNo 充值单号
     * @param channelTradeNo 渠道交易号
     * @param paidAt 支付时间
     * @return 充值单
     */
    RechargeOrder markAsPaid(Long tenantId, String rechargeNo, String channelTradeNo, LocalDateTime paidAt);
    
    /**
     * 根据渠道交易号标记为已支付（用于回调幂等）
     * 
     * @param tenantId 租户ID
     * @param channelTradeNo 渠道交易号
     * @param paidAt 支付时间
     * @return 充值单（如果找到）
     */
    RechargeOrder markAsPaidByChannelTradeNo(Long tenantId, String channelTradeNo, LocalDateTime paidAt);
    
    /**
     * 根据充值单号查询
     */
    RechargeOrder getByRechargeNo(Long tenantId, String rechargeNo);
    
    /**
     * 根据幂等键查询
     */
    RechargeOrder getByIdempotencyKey(Long tenantId, String idempotencyKey);
}
