package com.bluecone.app.wallet.application.consumer;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.infra.event.consume.EventConsumeLogService;
import com.bluecone.app.infra.event.outbox.DispatchedEvent;
import com.bluecone.app.wallet.domain.enums.BizType;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.model.WalletLedger;
import com.bluecone.app.wallet.domain.repository.WalletAccountRepository;
import com.bluecone.app.wallet.domain.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值事件消费者
 * 监听 RECHARGE_PAID 事件，执行钱包入账操作
 * 
 * 幂等性保证：
 * 1. 通过 bc_event_consume_log 表记录消费日志（consumer_name + event_id 唯一）
 * 2. 通过 bc_wallet_ledger 表的幂等键（idem_key）保证账本流水幂等
 * 3. 重复消费时直接返回，不会重复入账
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RechargeEventConsumer {
    
    private static final String CONSUMER_NAME = "RechargeConsumer";
    
    private final WalletAccountRepository walletAccountRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final EventConsumeLogService consumeLogService;
    private final IdService idService;
    
    /**
     * 监听充值支付成功事件：入账到钱包
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onRechargePaid(DispatchedEvent event) {
        if (!"recharge.paid".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        log.info("[{}] Received recharge.paid event: eventId={}", CONSUMER_NAME, eventId);
        
        // 1. 幂等性检查
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            // 2. 解析事件载荷
            String rechargeNo = getStringField(event.getPayload().get("rechargeNo"));
            Long rechargeId = getLongField(event.getPayload().get("rechargeId"));
            Long userId = getLongField(event.getPayload().get("userId"));
            Long accountId = getLongField(event.getPayload().get("accountId"));
            Long totalAmountInCents = getLongField(event.getPayload().get("totalAmount")); // 单位：分
            String channelTradeNo = getStringField(event.getPayload().get("channelTradeNo"));
            
            Long tenantId = event.getTenantId();
            
            log.info("[{}] Processing recharge: rechargeNo={}, userId={}, amount={} 分", 
                    CONSUMER_NAME, rechargeNo, userId, totalAmountInCents);
            
            // 3. 构建幂等键（基于充值单号）
            String idempotencyKey = buildIdempotencyKey(tenantId, userId, rechargeNo);
            
            // 4. 检查账本流水是否已存在（幂等）
            WalletLedger existingLedger = walletLedgerRepository.findByIdemKey(tenantId, idempotencyKey)
                    .orElse(null);
            
            if (existingLedger != null) {
                log.info("[{}] Ledger already exists (idempotent): rechargeNo={}, ledgerNo={}", 
                        CONSUMER_NAME, rechargeNo, existingLedger.getLedgerNo());
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, idempotencyKey, "LEDGER_EXISTS");
                return;
            }
            
            // 5. 查询钱包账户
            WalletAccount account = walletAccountRepository.findById(tenantId, accountId);
            if (account == null) {
                throw new BizException(com.bluecone.app.core.error.BizErrorCode.RESOURCE_NOT_FOUND, "钱包账户不存在：accountId=" + accountId);
            }
            
            // 6. 转换金额（分 -> 元）
            BigDecimal totalAmount = BigDecimal.valueOf(totalAmountInCents)
                    .divide(BigDecimal.valueOf(100));
            
            // 7. 记录变更前余额
            BigDecimal balanceBefore = account.getAvailableBalance();
            BigDecimal balanceAfter = balanceBefore.add(totalAmount);
            
            // 8. 生成账本流水
            Long ledgerId = idService.nextLong(IdScope.WALLET_LEDGER);
            String ledgerNo = idService.nextPublicId(ResourceType.WALLET_LEDGER);
            
            WalletLedger ledger = WalletLedger.builder()
                    .id(ledgerId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .accountId(accountId)
                    .ledgerNo(ledgerNo)
                    .bizType(BizType.RECHARGE.getCode())
                    .bizOrderId(rechargeId)
                    .bizOrderNo(rechargeNo)
                    .amount(totalAmount) // 正数=入账
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .currency("CNY")
                    .remark("充值入账：" + rechargeNo)
                    .idemKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 9. 写入账本流水（唯一约束兜底）
            try {
                walletLedgerRepository.save(ledger);
            } catch (DuplicateKeyException e) {
                // 并发情况下，唯一约束冲突，说明已经入账
                log.warn("[{}] Ledger duplicate key conflict (concurrent): rechargeNo={}, idempotencyKey={}", 
                        CONSUMER_NAME, rechargeNo, idempotencyKey);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, idempotencyKey, "DUPLICATE_KEY");
                return;
            }
            
            // 10. 更新账户余额（乐观锁）
            account.credit(totalAmount); // 增加可用余额
            account.addTotalRecharged(totalAmount); // 累计充值金额
            
            int updated = walletAccountRepository.updateWithVersion(account);
            if (updated == 0) {
                throw new BizException(com.bluecone.app.core.error.CommonErrorCode.CONFLICT, "账户余额更新失败（版本冲突），请重试");
            }
            
            // 11. 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, idempotencyKey, ledger);
            
            log.info("[{}] Recharge credited successfully: rechargeNo={}, userId={}, amount={} 元, " +
                    "balanceBefore={}, balanceAfter={}, ledgerNo={}", 
                    CONSUMER_NAME, rechargeNo, userId, totalAmount, balanceBefore, balanceAfter, ledgerNo);
            
        } catch (Exception e) {
            log.error("[{}] Failed to credit recharge: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e; // 让 Outbox 重试
        }
    }
    
    // ========== 私有方法 ==========
    
    private String buildIdempotencyKey(Long tenantId, Long userId, String rechargeNo) {
        return String.format("%d:%d:recharge:%s:credit", tenantId, userId, rechargeNo);
    }
    
    private Long getLongField(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String getStringField(Object value) {
        return value != null ? value.toString() : null;
    }
}
