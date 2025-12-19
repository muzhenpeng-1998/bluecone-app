package com.bluecone.app.wallet.api.facade;

/**
 * 钱包充值门面接口（预留）
 * 提供钱包充值、充值回调处理能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletRechargeFacade {
    
    /**
     * 创建充值单（预留，先空实现）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param rechargeAmount 充值金额
     * @param idempotencyKey 幂等键
     * @return 充值单号
     */
    String createRechargeOrder(Long tenantId, Long userId, 
                              java.math.BigDecimal rechargeAmount, 
                              String idempotencyKey);
    
    /**
     * 充值支付成功回调（预留，先空实现）
     * 
     * @param rechargeId 充值单号
     * @param payOrderId 支付单ID
     * @param payNo 第三方支付单号
     */
    void onRechargePaid(String rechargeId, Long payOrderId, String payNo);
}
