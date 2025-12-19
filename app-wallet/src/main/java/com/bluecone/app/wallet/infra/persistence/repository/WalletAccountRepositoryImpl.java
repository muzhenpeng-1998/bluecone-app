package com.bluecone.app.wallet.infra.persistence.repository;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.wallet.domain.enums.AccountStatus;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.repository.WalletAccountRepository;
import com.bluecone.app.wallet.infra.persistence.converter.WalletConverter;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletAccountMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletAccountPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账户仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletAccountRepositoryImpl implements WalletAccountRepository {
    
    private final WalletAccountMapper accountMapper;
    private final IdService idService;
    
    @Override
    public WalletAccount findByUserId(Long tenantId, Long userId) {
        WalletAccountPO po = accountMapper.selectByUserId(tenantId, userId);
        return WalletConverter.toDomain(po);
    }
    
    @Override
    public WalletAccount findById(Long tenantId, Long accountId) {
        WalletAccountPO po = accountMapper.selectById(accountId);
        if (po != null && !po.getTenantId().equals(tenantId)) {
            return null;
        }
        return WalletConverter.toDomain(po);
    }
    
    @Override
    public void insert(WalletAccount account) {
        WalletAccountPO po = WalletConverter.toPO(account);
        accountMapper.insert(po);
    }
    
    @Override
    public int updateWithVersion(WalletAccount account) {
        WalletAccountPO po = WalletConverter.toPO(account);
        return accountMapper.updateWithVersion(po);
    }
    
    @Override
    public WalletAccount getOrCreate(Long tenantId, Long userId) {
        WalletAccount existing = findByUserId(tenantId, userId);
        if (existing != null) {
            return existing;
        }
        
        // 创建新账户
        LocalDateTime now = LocalDateTime.now();
        WalletAccount newAccount = WalletAccount.builder()
                .id(idService.nextLong(IdScope.WALLET_ACCOUNT))
                .tenantId(tenantId)
                .userId(userId)
                .availableBalance(BigDecimal.ZERO)
                .frozenBalance(BigDecimal.ZERO)
                .totalRecharged(BigDecimal.ZERO)
                .totalConsumed(BigDecimal.ZERO)
                .currency("CNY")
                .status(AccountStatus.ACTIVE)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        try {
            insert(newAccount);
            log.info("创建钱包账户成功：tenantId={}, userId={}, accountId={}", tenantId, userId, newAccount.getId());
            return newAccount;
        } catch (Exception e) {
            // 并发创建时，可能唯一约束冲突，重新查询
            log.info("创建钱包账户冲突，重新查询：tenantId={}, userId={}", tenantId, userId);
            WalletAccount retry = findByUserId(tenantId, userId);
            if (retry != null) {
                return retry;
            }
            throw e;
        }
    }
}
