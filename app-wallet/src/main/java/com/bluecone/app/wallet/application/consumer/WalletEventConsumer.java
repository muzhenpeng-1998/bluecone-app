package com.bluecone.app.wallet.application.consumer;

import com.bluecone.app.infra.event.consume.EventConsumeLogService;
import com.bluecone.app.infra.event.outbox.DispatchedEvent;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 钱包事件消费者
 * 监听订单事件，执行钱包的冻结、提交、释放、回退等操作
 * 
 * 幂等性保证：
 * 1. 通过 bc_event_consume_log 表记录消费日志（consumer_name + event_id 唯一）
 * 2. 通过 Facade 层的幂等键（idempotencyKey）保证业务幂等性
 * 3. 重复消费时直接返回，不会重复执行业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventConsumer {
    
    private static final String CONSUMER_NAME = "WalletConsumer";
    
    private final WalletAssetFacade walletAssetFacade;
    private final EventConsumeLogService consumeLogService;
    
    /**
     * 监听订单结算锁定事件：冻结钱包余额
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCheckoutLocked(DispatchedEvent event) {
        if (!"order.checkout_locked".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        log.info("[{}] Received order.checkout_locked event: eventId={}", CONSUMER_NAME, eventId);
        
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            Long orderId = getLongField(event.getPayload().get("orderId"));
            BigDecimal walletAmount = getBigDecimalField(event.getPayload().get("walletAmount"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            // 如果没有使用钱包，直接返回
            if (walletAmount == null || walletAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("[{}] No wallet amount used, skip freezing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "freeze"), "NO_WALLET");
                return;
            }
            
            // 构建冻结命令
            WalletAssetCommand command = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(walletAmount)
                    .bizType("ORDER_PAY")
                    .bizId(String.valueOf(orderId))
                    .idempotencyKey(buildIdempotencyKey(orderId, "freeze"))
                    .remark("订单结算冻结")
                    .build();
            
            // 调用 Facade 冻结钱包
            WalletAssetResult result = walletAssetFacade.freeze(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Wallet frozen successfully: eventId={}, orderId={}, amount={}", 
                    CONSUMER_NAME, eventId, orderId, walletAmount);
            
        } catch (Exception e) {
            log.error("[{}] Failed to freeze wallet: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单支付成功事件：提交扣减钱包
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderPaid(DispatchedEvent event) {
        if (!"order.paid".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        log.info("[{}] Received order.paid event: eventId={}", CONSUMER_NAME, eventId);
        
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            Long orderId = getLongField(event.getPayload().get("orderId"));
            BigDecimal walletAmount = getBigDecimalField(event.getPayload().get("walletAmount"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            if (walletAmount == null || walletAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("[{}] No wallet amount used, skip committing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "commit"), "NO_WALLET");
                return;
            }
            
            // 构建提交命令
            WalletAssetCommand command = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(walletAmount)
                    .bizType("ORDER_PAY")
                    .bizId(String.valueOf(orderId))
                    .idempotencyKey(buildIdempotencyKey(orderId, "commit"))
                    .remark("订单支付扣减")
                    .build();
            
            // 调用 Facade 提交扣减
            WalletAssetResult result = walletAssetFacade.commit(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Wallet committed successfully: eventId={}, orderId={}, amount={}", 
                    CONSUMER_NAME, eventId, orderId, walletAmount);
            
        } catch (Exception e) {
            log.error("[{}] Failed to commit wallet: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单取消事件：释放钱包冻结
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCanceled(DispatchedEvent event) {
        if (!"order.canceled".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        log.info("[{}] Received order.canceled event: eventId={}", CONSUMER_NAME, eventId);
        
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            Long orderId = getLongField(event.getPayload().get("orderId"));
            BigDecimal walletAmount = getBigDecimalField(event.getPayload().get("walletAmount"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            if (walletAmount == null || walletAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("[{}] No wallet amount used, skip releasing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "release"), "NO_WALLET");
                return;
            }
            
            // 构建释放命令
            WalletAssetCommand command = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(walletAmount)
                    .bizType("ORDER_PAY")
                    .bizId(String.valueOf(orderId))
                    .idempotencyKey(buildIdempotencyKey(orderId, "release"))
                    .remark("订单取消释放")
                    .build();
            
            // 调用 Facade 释放冻结
            WalletAssetResult result = walletAssetFacade.release(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Wallet released successfully: eventId={}, orderId={}, amount={}", 
                    CONSUMER_NAME, eventId, orderId, walletAmount);
            
        } catch (Exception e) {
            log.error("[{}] Failed to release wallet: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单退款成功事件：回退钱包
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderRefunded(DispatchedEvent event) {
        if (!"order.refunded".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        log.info("[{}] Received order.refunded event: eventId={}", CONSUMER_NAME, eventId);
        
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            Long orderId = getLongField(event.getPayload().get("orderId"));
            Long refundId = getLongField(event.getPayload().get("refundId"));
            BigDecimal refundAmount = getBigDecimalField(event.getPayload().get("refundAmount"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("[{}] No refund amount, skip reverting: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(refundId, "revert"), "NO_REFUND");
                return;
            }
            
            // 构建回退命令
            WalletAssetCommand command = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(refundAmount)
                    .bizType("ORDER_REFUND")
                    .bizId(String.valueOf(refundId))
                    .idempotencyKey(buildIdempotencyKey(refundId, "revert"))
                    .remark("订单退款返还")
                    .build();
            
            // 调用 Facade 回退钱包
            WalletAssetResult result = walletAssetFacade.revert(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Wallet reverted successfully: eventId={}, orderId={}, refundId={}, amount={}", 
                    CONSUMER_NAME, eventId, orderId, refundId, refundAmount);
            
        } catch (Exception e) {
            log.error("[{}] Failed to revert wallet: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    private String buildIdempotencyKey(Long bizId, String action) {
        return String.format("wallet:%s:%s", bizId, action);
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
    
    private BigDecimal getBigDecimalField(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
