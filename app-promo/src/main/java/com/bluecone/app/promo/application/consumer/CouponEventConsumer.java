package com.bluecone.app.promo.application.consumer;

import com.bluecone.app.infra.event.consume.EventConsumeLogService;
import com.bluecone.app.infra.event.outbox.DispatchedEvent;
import com.bluecone.app.promo.api.dto.CouponCommitCommand;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.dto.CouponReleaseCommand;
import com.bluecone.app.promo.api.facade.CouponLockFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 优惠券事件消费者
 * 监听订单事件，执行优惠券的锁定、核销、释放等操作
 * 
 * 幂等性保证：
 * 1. 通过 bc_event_consume_log 表记录消费日志（consumer_name + event_id 唯一）
 * 2. 通过 Facade 层的幂等键（idempotencyKey）保证业务幂等性
 * 3. 重复消费时直接返回，不会重复执行业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {
    
    private static final String CONSUMER_NAME = "CouponConsumer";
    
    private final CouponLockFacade couponLockFacade;
    private final EventConsumeLogService consumeLogService;
    
    /**
     * 监听订单结算锁定事件：锁定优惠券
     * 
     * @param event 分发的事件
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCheckoutLocked(DispatchedEvent event) {
        if (!"order.checkout_locked".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        String traceId = event.getTraceId();
        
        log.info("[{}] Received order.checkout_locked event: eventId={}, traceId={}", 
                CONSUMER_NAME, eventId, traceId);
        
        // 幂等性检查：如果已消费，直接返回
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            // 解析事件载荷
            Long orderId = getLongField(event.getPayload().get("orderId"));
            Long couponId = getLongField(event.getPayload().get("couponId"));
            Long tenantId = event.getTenantId();
            Long storeId = event.getStoreId();
            Long userId = event.getUserId();
            
            // 如果没有使用优惠券，直接返回
            if (couponId == null) {
                log.info("[{}] No coupon used, skip locking: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "checkout"), "NO_COUPON");
                return;
            }
            
            // 构建锁定命令
            CouponLockCommand command = new CouponLockCommand();
            command.setTenantId(tenantId);
            command.setStoreId(storeId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setIdempotencyKey(buildIdempotencyKey(orderId, "checkout"));
            
            // 调用 Facade 锁定优惠券
            CouponLockResult result = couponLockFacade.lock(command);
            
            // 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Coupon locked successfully: eventId={}, orderId={}, couponId={}, lockId={}", 
                    CONSUMER_NAME, eventId, orderId, couponId, result.getLockId());
            
        } catch (Exception e) {
            log.error("[{}] Failed to lock coupon: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            
            // 记录消费失败（由 Outbox 重试机制驱动重新消费）
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            
            throw e;  // 抛出异常，让 Outbox 重试
        }
    }
    
    /**
     * 监听订单支付成功事件：核销优惠券
     * 
     * @param event 分发的事件
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderPaid(DispatchedEvent event) {
        if (!"order.paid".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        String traceId = event.getTraceId();
        
        log.info("[{}] Received order.paid event: eventId={}, traceId={}", 
                CONSUMER_NAME, eventId, traceId);
        
        // 幂等性检查
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            // 解析事件载荷
            Long orderId = getLongField(event.getPayload().get("orderId"));
            Long couponId = getLongField(event.getPayload().get("couponId"));
            Long tenantId = event.getTenantId();
            Long storeId = event.getStoreId();
            Long userId = event.getUserId();
            
            // 如果没有使用优惠券，直接返回
            if (couponId == null) {
                log.info("[{}] No coupon used, skip committing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "commit"), "NO_COUPON");
                return;
            }
            
            // 构建核销命令
            CouponCommitCommand command = new CouponCommitCommand();
            command.setTenantId(tenantId);
            command.setStoreId(storeId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setIdempotencyKey(buildIdempotencyKey(orderId, "commit"));
            
            // 调用 Facade 核销优惠券
            couponLockFacade.commit(command);
            
            // 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), "SUCCESS");
            
            log.info("[{}] Coupon committed successfully: eventId={}, orderId={}, couponId={}", 
                    CONSUMER_NAME, eventId, orderId, couponId);
            
        } catch (Exception e) {
            log.error("[{}] Failed to commit coupon: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 监听订单取消事件：释放优惠券
     * 
     * @param event 分发的事件
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCanceled(DispatchedEvent event) {
        if (!"order.canceled".equals(event.getEventType())) {
            return;
        }
        
        String eventId = event.getEventId();
        String traceId = event.getTraceId();
        
        log.info("[{}] Received order.canceled event: eventId={}, traceId={}", 
                CONSUMER_NAME, eventId, traceId);
        
        // 幂等性检查
        if (consumeLogService.isConsumed(CONSUMER_NAME, eventId)) {
            log.info("[{}] Event already consumed (idempotent): eventId={}", CONSUMER_NAME, eventId);
            return;
        }
        
        try {
            // 解析事件载荷
            Long orderId = getLongField(event.getPayload().get("orderId"));
            Long couponId = getLongField(event.getPayload().get("couponId"));
            Long tenantId = event.getTenantId();
            Long storeId = event.getStoreId();
            Long userId = event.getUserId();
            
            // 如果没有使用优惠券，直接返回
            if (couponId == null) {
                log.info("[{}] No coupon used, skip releasing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "release"), "NO_COUPON");
                return;
            }
            
            // 构建释放命令
            CouponReleaseCommand command = new CouponReleaseCommand();
            command.setTenantId(tenantId);
            command.setStoreId(storeId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setIdempotencyKey(buildIdempotencyKey(orderId, "release"));
            
            // 调用 Facade 释放优惠券
            couponLockFacade.release(command);
            
            // 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), "SUCCESS");
            
            log.info("[{}] Coupon released successfully: eventId={}, orderId={}, couponId={}", 
                    CONSUMER_NAME, eventId, orderId, couponId);
            
        } catch (Exception e) {
            log.error("[{}] Failed to release coupon: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            
            throw e;
        }
    }
    
    /**
     * 构建幂等键
     */
    private String buildIdempotencyKey(Long orderId, String action) {
        return String.format("order:%s:%s", orderId, action);
    }
    
    /**
     * 安全获取 Long 字段
     */
    private Long getLongField(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
