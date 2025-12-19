package com.bluecone.app.wallet.domain.service.impl;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.wallet.domain.enums.RechargeStatus;
import com.bluecone.app.wallet.domain.model.RechargeOrder;
import com.bluecone.app.wallet.domain.repository.RechargeOrderRepository;
import com.bluecone.app.wallet.domain.service.RechargeOrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 充值单领域服务实现
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeOrderDomainServiceImpl implements RechargeOrderDomainService {
    
    private final RechargeOrderRepository rechargeOrderRepository;
    private final IdService idService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRechargeOrder(Long tenantId, Long userId, Long accountId,
                                            Long rechargeAmountInCents, Long bonusAmountInCents,
                                            String idempotencyKey) {
        // 1. 幂等性检查
        RechargeOrder existing = rechargeOrderRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            log.info("充值单已存在（幂等）：rechargeNo={}, idempotencyKey={}", 
                    existing.getRechargeNo(), idempotencyKey);
            return existing;
        }
        
        // 2. 参数校验
        if (rechargeAmountInCents == null || rechargeAmountInCents <= 0) {
            throw new BusinessException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "充值金额必须大于0");
        }
        
        // 3. 生成充值单
        Long rechargeId = idService.nextLong(IdScope.WALLET_RECHARGE);
        String rechargeNo = idService.nextPublicId(ResourceType.WALLET_RECHARGE);
        
        Long totalAmount = rechargeAmountInCents + (bonusAmountInCents != null ? bonusAmountInCents : 0L);
        
        RechargeOrder rechargeOrder = RechargeOrder.builder()
                .id(rechargeId)
                .tenantId(tenantId)
                .userId(userId)
                .accountId(accountId)
                .rechargeNo(rechargeNo)
                .rechargeAmount(rechargeAmountInCents)
                .bonusAmount(bonusAmountInCents != null ? bonusAmountInCents : 0L)
                .totalAmount(totalAmount)
                .currency("CNY")
                .status(RechargeStatus.INIT)
                .requestedAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .version(0)
                .build();
        
        // 4. 保存充值单（唯一约束兜底）
        try {
            rechargeOrderRepository.save(rechargeOrder);
            log.info("创建充值单成功：rechargeNo={}, amount={} 分", rechargeNo, rechargeAmountInCents);
            return rechargeOrder;
        } catch (DuplicateKeyException e) {
            // 并发情况下，唯一约束冲突，重新查询返回
            log.warn("充值单创建冲突（并发），重新查询：idempotencyKey={}", idempotencyKey);
            return rechargeOrderRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(com.bluecone.app.core.error.CommonErrorCode.SYSTEM_ERROR, "充值单创建失败"));
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPaying(Long tenantId, String rechargeNo, Long payOrderId, String payChannel) {
        RechargeOrder rechargeOrder = getByRechargeNo(tenantId, rechargeNo);
        
        // 幂等性：如果已经是 PAYING 或更后面的状态，直接返回
        if (rechargeOrder.getStatus() == RechargeStatus.PAYING) {
            log.info("充值单已标记为支付中（幂等）：rechargeNo={}", rechargeNo);
            return;
        }
        
        if (rechargeOrder.getStatus().isFinalState()) {
            log.warn("充值单已完成，无法标记为支付中：rechargeNo={}, status={}", 
                    rechargeNo, rechargeOrder.getStatus());
            return;
        }
        
        // 状态流转
        rechargeOrder.markAsPaying(payOrderId, payChannel);
        
        // 乐观锁更新
        int updated = rechargeOrderRepository.updateWithVersion(rechargeOrder);
        if (updated == 0) {
            throw new BusinessException(com.bluecone.app.core.error.CommonErrorCode.CONFLICT, "充值单状态更新失败（版本冲突），请重试");
        }
        
        log.info("充值单标记为支付中：rechargeNo={}, payOrderId={}, payChannel={}", 
                rechargeNo, payOrderId, payChannel);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder markAsPaid(Long tenantId, String rechargeNo, String channelTradeNo, LocalDateTime paidAt) {
        RechargeOrder rechargeOrder = getByRechargeNo(tenantId, rechargeNo);
        return markOrderAsPaid(rechargeOrder, channelTradeNo, paidAt);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder markAsPaidByChannelTradeNo(Long tenantId, String channelTradeNo, LocalDateTime paidAt) {
        // 先尝试根据渠道交易号查询（用于回调幂等）
        RechargeOrder existing = rechargeOrderRepository.findByChannelTradeNo(tenantId, channelTradeNo)
                .orElse(null);
        
        if (existing != null) {
            log.info("根据渠道交易号找到充值单：rechargeNo={}, channelTradeNo={}", 
                    existing.getRechargeNo(), channelTradeNo);
            
            // 如果已经是 PAID 状态，直接返回（幂等）
            if (existing.getStatus() == RechargeStatus.PAID) {
                log.info("充值单已支付（幂等）：rechargeNo={}, channelTradeNo={}", 
                        existing.getRechargeNo(), channelTradeNo);
                return existing;
            }
            
            return markOrderAsPaid(existing, channelTradeNo, paidAt);
        }
        
        log.warn("未找到对应的充值单：channelTradeNo={}", channelTradeNo);
        return null;
    }
    
    @Override
    public RechargeOrder getByRechargeNo(Long tenantId, String rechargeNo) {
        return rechargeOrderRepository.findByRechargeNo(tenantId, rechargeNo)
                .orElseThrow(() -> new BusinessException(com.bluecone.app.core.error.BizErrorCode.RESOURCE_NOT_FOUND, "充值单不存在：" + rechargeNo));
    }
    
    @Override
    public RechargeOrder getByIdempotencyKey(Long tenantId, String idempotencyKey) {
        return rechargeOrderRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                .orElse(null);
    }
    
    // ========== 私有方法 ==========
    
    private RechargeOrder markOrderAsPaid(RechargeOrder rechargeOrder, String channelTradeNo, LocalDateTime paidAt) {
        // 幂等性：如果已经是 PAID 状态，直接返回
        if (rechargeOrder.getStatus() == RechargeStatus.PAID) {
            log.info("充值单已支付（幂等）：rechargeNo={}", rechargeOrder.getRechargeNo());
            return rechargeOrder;
        }
        
        if (rechargeOrder.getStatus() == RechargeStatus.CLOSED) {
            throw new BusinessException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "充值单已关闭，无法标记为已支付：" + rechargeOrder.getRechargeNo());
        }
        
        // 状态流转
        rechargeOrder.markAsPaid(channelTradeNo, paidAt != null ? paidAt : LocalDateTime.now());
        
        // 乐观锁更新
        int updated = rechargeOrderRepository.updateWithVersion(rechargeOrder);
        if (updated == 0) {
            throw new BusinessException(com.bluecone.app.core.error.CommonErrorCode.CONFLICT, "充值单状态更新失败（版本冲突），请重试");
        }
        
        log.info("充值单标记为已支付：rechargeNo={}, channelTradeNo={}, paidAt={}", 
                rechargeOrder.getRechargeNo(), channelTradeNo, paidAt);
        
        return rechargeOrder;
    }
}
