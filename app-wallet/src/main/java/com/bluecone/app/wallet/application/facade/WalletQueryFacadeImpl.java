package com.bluecone.app.wallet.application.facade;

import com.bluecone.app.wallet.api.dto.WalletBalanceDTO;
import com.bluecone.app.wallet.api.facade.WalletQueryFacade;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.service.WalletDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 钱包查询门面实现
 * 提供钱包余额查询能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletQueryFacadeImpl implements WalletQueryFacade {
    
    private final WalletDomainService walletDomainService;
    
    /**
     * 查询用户钱包余额
     */
    @Override
    public WalletBalanceDTO getBalance(Long tenantId, Long userId) {
        WalletAccount account = walletDomainService.getAccount(tenantId, userId);
        if (account == null) {
            return null;
        }
        return toBalanceDTO(account);
    }
    
    /**
     * 查询或创建用户钱包账户（如果不存在则创建）
     */
    @Override
    public WalletBalanceDTO getOrCreateBalance(Long tenantId, Long userId) {
        WalletAccount account = walletDomainService.getOrCreateAccount(tenantId, userId);
        return toBalanceDTO(account);
    }
    
    /**
     * 检查用户余额是否足够
     */
    @Override
    public boolean hasEnoughBalance(Long tenantId, Long userId, BigDecimal requiredAmount) {
        if (requiredAmount == null || requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        
        WalletAccount account = walletDomainService.getAccount(tenantId, userId);
        if (account == null) {
            return false;
        }
        
        return account.hasEnoughBalance(requiredAmount);
    }
    
    // ==================== 私有方法 ====================
    
    private WalletBalanceDTO toBalanceDTO(WalletAccount account) {
        if (account == null) {
            return null;
        }
        
        return new WalletBalanceDTO(
                account.getId(),
                account.getTenantId(),
                account.getUserId(),
                account.getAvailableBalance(),
                account.getFrozenBalance(),
                account.getCurrency(),
                account.getStatus() != null ? account.getStatus().getCode() : null,
                account.getVersion()
        );
    }
}
