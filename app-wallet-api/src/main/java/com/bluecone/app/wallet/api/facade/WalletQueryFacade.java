package com.bluecone.app.wallet.api.facade;

import com.bluecone.app.wallet.api.dto.WalletBalanceDTO;

/**
 * 钱包查询门面接口
 * 提供钱包余额查询能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletQueryFacade {
    
    /**
     * 查询用户钱包余额
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 钱包余额（如果账户不存在则返回null）
     */
    WalletBalanceDTO getBalance(Long tenantId, Long userId);
    
    /**
     * 查询或创建用户钱包账户（如果不存在则创建）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 钱包余额
     */
    WalletBalanceDTO getOrCreateBalance(Long tenantId, Long userId);
    
    /**
     * 检查用户余额是否足够
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param requiredAmount 需要的金额
     * @return true=余额足够，false=余额不足
     */
    boolean hasEnoughBalance(Long tenantId, Long userId, java.math.BigDecimal requiredAmount);
}
