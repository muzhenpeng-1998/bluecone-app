package com.bluecone.app.wallet.domain.repository;

import com.bluecone.app.wallet.domain.model.WalletFreeze;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 钱包冻结记录仓储接口
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletFreezeRepository {
    
    /**
     * 根据幂等键查询冻结记录
     */
    WalletFreeze findByIdemKey(Long tenantId, String idemKey);
    
    /**
     * 根据业务单ID查询冻结记录
     */
    WalletFreeze findByBizOrderId(Long tenantId, String bizType, Long bizOrderId);
    
    /**
     * 创建冻结记录
     */
    void insert(WalletFreeze freeze);
    
    /**
     * 更新冻结记录（使用乐观锁）
     * @return 更新行数（0=乐观锁冲突，1=更新成功）
     */
    int updateWithVersion(WalletFreeze freeze);
    
    /**
     * 查询过期的冻结记录（用于超时释放）
     */
    List<WalletFreeze> findExpiredFreezes(LocalDateTime now, int limit);
}
