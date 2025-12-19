package com.bluecone.app.wallet.api.facade;

import com.bluecone.app.wallet.api.dto.RechargeCreateCommand;
import com.bluecone.app.wallet.api.dto.RechargeCreateResult;

/**
 * 钱包充值门面接口
 * 提供钱包充值、充值回调处理能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletRechargeFacade {
    
    /**
     * 创建充值单
     * 
     * @param command 充值命令
     * @return 充值结果（包含充值单号、支付参数等）
     */
    RechargeCreateResult createRechargeOrder(RechargeCreateCommand command);
    
    /**
     * 充值支付成功回调
     * 
     * @param tenantId 租户ID
     * @param channelTradeNo 渠道交易号
     * @param paidAt 支付时间
     */
    void onRechargePaid(Long tenantId, String channelTradeNo, java.time.LocalDateTime paidAt);
}
