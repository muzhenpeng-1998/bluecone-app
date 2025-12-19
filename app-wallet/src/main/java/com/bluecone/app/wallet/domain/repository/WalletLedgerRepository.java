package com.bluecone.app.wallet.domain.repository;

import com.bluecone.app.wallet.domain.model.WalletLedger;

import java.util.List;

/**
 * 钱包账本流水仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletLedgerRepository {
    
    /**
     * 根据幂等键查询流水
     */
    WalletLedger findByIdemKey(Long tenantId, String idemKey);
    
    /**
     * 创建流水记录
     */
    void insert(WalletLedger ledger);
    
    /**
     * 根据用户ID分页查询流水
     */
    List<WalletLedger> findByUserId(Long tenantId, Long userId, int offset, int limit);
    
    /**
     * 根据账户ID分页查询流水
     */
    List<WalletLedger> findByAccountId(Long tenantId, Long accountId, int offset, int limit);
}
