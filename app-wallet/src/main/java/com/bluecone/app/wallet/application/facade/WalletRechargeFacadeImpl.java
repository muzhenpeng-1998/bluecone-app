package com.bluecone.app.wallet.application.facade;

import com.bluecone.app.core.event.outbox.AggregateType;
import com.bluecone.app.core.event.outbox.EventType;
import com.bluecone.app.core.event.outbox.OutboxEvent;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.event.outbox.OutboxEventService;
import com.bluecone.app.wallet.api.dto.RechargeCreateCommand;
import com.bluecone.app.wallet.api.dto.RechargeCreateResult;
import com.bluecone.app.wallet.api.facade.WalletRechargeFacade;
import com.bluecone.app.wallet.domain.model.RechargeOrder;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.repository.WalletAccountRepository;
import com.bluecone.app.wallet.domain.service.RechargeOrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 钱包充值门面实现
 * 提供钱包充值、充值回调处理能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletRechargeFacadeImpl implements WalletRechargeFacade {
    
    private final RechargeOrderDomainService rechargeOrderDomainService;
    private final WalletAccountRepository walletAccountRepository;
    private final OutboxEventService outboxEventService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeCreateResult createRechargeOrder(RechargeCreateCommand command) {
        // 1. 参数校验
        validateCommand(command);
        
        // 2. 获取或创建钱包账户
        WalletAccount account = walletAccountRepository.getOrCreate(
                command.getTenantId(), 
                command.getUserId()
        );
        
        // 3. 转换金额为分
        Long rechargeAmountInCents = command.getRechargeAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();
        
        // 4. 创建充值单（幂等）
        RechargeOrder rechargeOrder = rechargeOrderDomainService.createRechargeOrder(
                command.getTenantId(),
                command.getUserId(),
                account.getId(),
                rechargeAmountInCents,
                0L, // 暂不支持赠送金额
                command.getIdempotencyKey()
        );
        
        log.info("创建充值单成功：rechargeNo={}, amount={} 元, userId={}", 
                rechargeOrder.getRechargeNo(), command.getRechargeAmount(), command.getUserId());
        
        // 5. 构建返回结果（暂不集成支付，返回充值单信息）
        return RechargeCreateResult.builder()
                .rechargeNo(rechargeOrder.getRechargeNo())
                .rechargeAmount(rechargeOrder.getRechargeAmountInYuan())
                .bonusAmount(rechargeOrder.getBonusAmountInYuan())
                .totalAmount(rechargeOrder.getTotalAmountInYuan())
                .payChannel(command.getPayChannel())
                .status(rechargeOrder.getStatus().getCode())
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRechargePaid(Long tenantId, String channelTradeNo, LocalDateTime paidAt) {
        log.info("处理充值支付回调：tenantId={}, channelTradeNo={}, paidAt={}", 
                tenantId, channelTradeNo, paidAt);
        
        // 1. 根据渠道交易号查询并标记充值单为已支付（幂等）
        RechargeOrder rechargeOrder = rechargeOrderDomainService.markAsPaidByChannelTradeNo(
                tenantId, 
                channelTradeNo, 
                paidAt
        );
        
        if (rechargeOrder == null) {
            log.warn("未找到对应的充值单，跳过处理：channelTradeNo={}", channelTradeNo);
            return;
        }
        
        log.info("充值单标记为已支付：rechargeNo={}, channelTradeNo={}", 
                rechargeOrder.getRechargeNo(), channelTradeNo);
        
        // 2. 写入 Outbox 事件：RECHARGE_PAID（同事务）
        Map<String, Object> payload = new HashMap<>();
        payload.put("rechargeNo", rechargeOrder.getRechargeNo());
        payload.put("rechargeId", rechargeOrder.getId());
        payload.put("userId", rechargeOrder.getUserId());
        payload.put("accountId", rechargeOrder.getAccountId());
        payload.put("rechargeAmount", rechargeOrder.getRechargeAmount()); // 单位：分
        payload.put("bonusAmount", rechargeOrder.getBonusAmount()); // 单位：分
        payload.put("totalAmount", rechargeOrder.getTotalAmount()); // 单位：分
        payload.put("channelTradeNo", channelTradeNo);
        payload.put("paidAt", paidAt != null ? paidAt.toString() : LocalDateTime.now().toString());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", rechargeOrder.getUserId());
        metadata.put("source", "recharge_callback");
        
        OutboxEvent event = OutboxEvent.builder()
                .tenantId(tenantId)
                .storeId(0L) // 充值不关联门店
                .aggregateType(AggregateType.WALLET)
                .aggregateId(rechargeOrder.getRechargeNo())
                .eventType(EventType.RECHARGE_PAID)
                .eventId(java.util.UUID.randomUUID().toString())
                .payload(payload)
                .metadata(metadata)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        outboxEventService.save(event);
        
        log.info("充值支付成功事件已写入 Outbox：rechargeNo={}, eventId={}", 
                rechargeOrder.getRechargeNo(), event.getEventId());
    }
    
    // ========== 私有方法 ==========
    
    private void validateCommand(RechargeCreateCommand command) {
        if (command.getTenantId() == null) {
            throw new BizException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (command.getUserId() == null) {
            throw new BizException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "用户ID不能为空");
        }
        if (command.getRechargeAmount() == null || command.getRechargeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "充值金额必须大于0");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().trim().isEmpty()) {
            throw new BizException(com.bluecone.app.core.error.BizErrorCode.INVALID_PARAM, "幂等键不能为空");
        }
    }
}
