package com.bluecone.app.wallet.application.facade;

import com.bluecone.app.wallet.api.facade.WalletRechargeFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 钱包充值门面实现（预留）
 * 提供钱包充值、充值回调处理能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletRechargeFacadeImpl implements WalletRechargeFacade {
    
    /**
     * 创建充值单（预留，先空实现）
     */
    @Override
    public String createRechargeOrder(Long tenantId, Long userId, 
                                     BigDecimal rechargeAmount, 
                                     String idempotencyKey) {
        log.warn("createRechargeOrder 未实现：tenantId={}, userId={}, amount={}", 
                tenantId, userId, rechargeAmount);
        throw new UnsupportedOperationException("充值功能暂未实现");
    }
    
    /**
     * 充值支付成功回调（预留，先空实现）
     */
    @Override
    public void onRechargePaid(String rechargeId, Long payOrderId, String payNo) {
        log.warn("onRechargePaid 未实现：rechargeId={}, payOrderId={}, payNo={}", 
                rechargeId, payOrderId, payNo);
        throw new UnsupportedOperationException("充值功能暂未实现");
    }
}
