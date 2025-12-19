package com.bluecone.app.wallet.infra.persistence.converter;

import com.bluecone.app.wallet.domain.enums.AccountStatus;
import com.bluecone.app.wallet.domain.enums.FreezeStatus;
import com.bluecone.app.wallet.domain.enums.RechargeStatus;
import com.bluecone.app.wallet.domain.model.RechargeOrder;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.model.WalletFreeze;
import com.bluecone.app.wallet.domain.model.WalletLedger;
import com.bluecone.app.wallet.infra.persistence.po.RechargeOrderPO;
import com.bluecone.app.wallet.infra.persistence.po.WalletAccountPO;
import com.bluecone.app.wallet.infra.persistence.po.WalletFreezePO;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;

import java.math.BigDecimal;

/**
 * 钱包领域模型与PO转换器
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class WalletConverter {
    
    // ============ WalletAccount ============
    
    public static WalletAccount toDomain(WalletAccountPO po) {
        if (po == null) {
            return null;
        }
        return WalletAccount.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .availableBalance(po.getAvailableBalance())
                .frozenBalance(po.getFrozenBalance())
                .totalRecharged(po.getTotalRecharged())
                .totalConsumed(po.getTotalConsumed())
                .currency(po.getCurrency())
                .status(AccountStatus.fromCode(po.getStatus()))
                .version(po.getVersion())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }
    
    public static WalletAccountPO toPO(WalletAccount domain) {
        if (domain == null) {
            return null;
        }
        WalletAccountPO po = new WalletAccountPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setUserId(domain.getUserId());
        po.setAvailableBalance(domain.getAvailableBalance());
        po.setFrozenBalance(domain.getFrozenBalance());
        po.setTotalRecharged(domain.getTotalRecharged());
        po.setTotalConsumed(domain.getTotalConsumed());
        po.setCurrency(domain.getCurrency());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }
    
    // ============ WalletFreeze ============
    
    public static WalletFreeze toFreezeDomain(WalletFreezePO po) {
        if (po == null) {
            return null;
        }
        return WalletFreeze.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .accountId(po.getAccountId())
                .freezeNo(po.getFreezeNo())
                .bizType(po.getBizType())
                .bizOrderId(po.getBizOrderId())
                .bizOrderNo(po.getBizOrderNo())
                .frozenAmount(po.getFrozenAmount())
                .currency(po.getCurrency())
                .status(FreezeStatus.fromCode(po.getStatus()))
                .idemKey(po.getIdemKey())
                .frozenAt(po.getFrozenAt())
                .expiresAt(po.getExpiresAt())
                .committedAt(po.getCommittedAt())
                .releasedAt(po.getReleasedAt())
                .revertedAt(po.getRevertedAt())
                .version(po.getVersion())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }
    
    public static WalletFreezePO toFreezePO(WalletFreeze domain) {
        if (domain == null) {
            return null;
        }
        WalletFreezePO po = new WalletFreezePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setUserId(domain.getUserId());
        po.setAccountId(domain.getAccountId());
        po.setFreezeNo(domain.getFreezeNo());
        po.setBizType(domain.getBizType());
        po.setBizOrderId(domain.getBizOrderId());
        po.setBizOrderNo(domain.getBizOrderNo());
        po.setFrozenAmount(domain.getFrozenAmount());
        po.setCurrency(domain.getCurrency());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setIdemKey(domain.getIdemKey());
        po.setFrozenAt(domain.getFrozenAt());
        po.setExpiresAt(domain.getExpiresAt());
        po.setCommittedAt(domain.getCommittedAt());
        po.setReleasedAt(domain.getReleasedAt());
        po.setRevertedAt(domain.getRevertedAt());
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }
    
    // ============ WalletLedger ============
    
    public static WalletLedger toLedgerDomain(WalletLedgerPO po) {
        if (po == null) {
            return null;
        }
        return WalletLedger.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .accountId(po.getAccountId())
                .ledgerNo(po.getLedgerNo())
                .bizType(po.getBizType())
                .bizOrderId(po.getBizOrderId())
                .bizOrderNo(po.getBizOrderNo())
                .amount(po.getAmount())
                .balanceBefore(po.getBalanceBefore())
                .balanceAfter(po.getBalanceAfter())
                .currency(po.getCurrency())
                .remark(po.getRemark())
                .idemKey(po.getIdemKey())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .build();
    }
    
    public static WalletLedgerPO toLedgerPO(WalletLedger domain) {
        if (domain == null) {
            return null;
        }
        WalletLedgerPO po = new WalletLedgerPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setUserId(domain.getUserId());
        po.setAccountId(domain.getAccountId());
        po.setLedgerNo(domain.getLedgerNo());
        po.setBizType(domain.getBizType());
        po.setBizOrderId(domain.getBizOrderId());
        po.setBizOrderNo(domain.getBizOrderNo());
        po.setAmount(domain.getAmount());
        po.setBalanceBefore(domain.getBalanceBefore());
        po.setBalanceAfter(domain.getBalanceAfter());
        po.setCurrency(domain.getCurrency());
        po.setRemark(domain.getRemark());
        po.setIdemKey(domain.getIdemKey());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        return po;
    }
    
    // ============ RechargeOrder ============
    
    public static RechargeOrder toRechargeOrderDomain(RechargeOrderPO po) {
        if (po == null) {
            return null;
        }
        return RechargeOrder.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .userId(po.getUserId())
                .accountId(po.getAccountId())
                .rechargeNo(po.getRechargeNo())
                .rechargeAmount(po.getRechargeAmount() != null ? 
                        po.getRechargeAmount().multiply(BigDecimal.valueOf(100)).longValue() : 0L)
                .bonusAmount(po.getBonusAmount() != null ? 
                        po.getBonusAmount().multiply(BigDecimal.valueOf(100)).longValue() : 0L)
                .totalAmount(po.getTotalAmount() != null ? 
                        po.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue() : 0L)
                .currency(po.getCurrency())
                .status(RechargeStatus.fromCode(po.getStatus()))
                .payOrderId(po.getPayOrderId())
                .payChannel(po.getPayChannel())
                .channelTradeNo(po.getPayNo())
                .requestedAt(po.getRechargeRequestedAt())
                .paidAt(po.getRechargeCompletedAt())
                .idempotencyKey(po.getIdemKey())
                .extJson(po.getExtJson())
                .version(po.getVersion())
                .createdAt(po.getCreatedAt())
                .createdBy(po.getCreatedBy())
                .updatedAt(po.getUpdatedAt())
                .updatedBy(po.getUpdatedBy())
                .build();
    }
    
    public static RechargeOrderPO toRechargeOrderPO(RechargeOrder domain) {
        if (domain == null) {
            return null;
        }
        RechargeOrderPO po = new RechargeOrderPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setUserId(domain.getUserId());
        po.setAccountId(domain.getAccountId());
        po.setRechargeNo(domain.getRechargeNo());
        // 转换分为元
        po.setRechargeAmount(domain.getRechargeAmount() != null ? 
                BigDecimal.valueOf(domain.getRechargeAmount()).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
        po.setBonusAmount(domain.getBonusAmount() != null ? 
                BigDecimal.valueOf(domain.getBonusAmount()).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
        po.setTotalAmount(domain.getTotalAmount() != null ? 
                BigDecimal.valueOf(domain.getTotalAmount()).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
        po.setCurrency(domain.getCurrency());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().getCode() : null);
        po.setPayOrderId(domain.getPayOrderId());
        po.setPayChannel(domain.getPayChannel());
        po.setPayNo(domain.getChannelTradeNo());
        po.setRechargeRequestedAt(domain.getRequestedAt());
        po.setRechargeCompletedAt(domain.getPaidAt());
        po.setIdemKey(domain.getIdempotencyKey());
        po.setExtJson(domain.getExtJson());
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }
}
