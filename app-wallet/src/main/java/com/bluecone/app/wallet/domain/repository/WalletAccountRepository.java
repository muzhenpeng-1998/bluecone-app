package com.bluecone.app.wallet.domain.repository;

import com.bluecone.app.wallet.domain.model.WalletAccount;

/**
 * 钱包账户仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletAccountRepository {
    
    /**
     * 根据用户ID查询账户
     */
    WalletAccount findByUserId(Long tenantId, Long userId);
    
    /**
     * 根据账户ID查询账户
     */
    WalletAccount findById(Long tenantId, Long accountId);
    
    /**
     * 创建账户
     */
    void insert(WalletAccount account);
    
    /**
     * 更新账户（使用乐观锁）
     * @return 更新行数（0=乐观锁冲突，1=更新成功）
     */
    int updateWithVersion(WalletAccount account);
    
    /**
     * 创建或获取账户（如果不存在则创建）
     */
    WalletAccount getOrCreate(Long tenantId, Long userId);
}
