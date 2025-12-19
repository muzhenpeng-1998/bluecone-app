package com.bluecone.app.member.application.consumer;

import com.bluecone.app.infra.event.consume.EventConsumeLogService;
import com.bluecone.app.infra.event.outbox.DispatchedEvent;
import com.bluecone.app.member.api.dto.PointsOperationCommand;
import com.bluecone.app.member.api.dto.PointsOperationResult;
import com.bluecone.app.member.api.facade.PointsAssetFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分事件消费者
 * 监听订单事件，执行积分的冻结、提交、释放、回退、赚取等操作
 * 
 * 幂等性保证：
 * 1. 通过 bc_event_consume_log 表记录消费日志（consumer_name + event_id 唯一）
 * 2. 通过 Facade 层的幂等键（idempotencyKey）保证业务幂等性
 * 3. 重复消费时直接返回，不会重复执行业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsEventConsumer {
    
    private static final String CONSUMER_NAME = "PointsConsumer";
    
    private final PointsAssetFacade pointsAssetFacade;
    private final EventConsumeLogService consumeLogService;
    
    /**
     * 监听订单结算锁定事件：冻结积分（如果使用积分抵扣）
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
            Integer pointsUsed = getIntegerField(event.getPayload().get("pointsUsed"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            // 如果没有使用积分，直接返回
            if (pointsUsed == null || pointsUsed <= 0) {
                log.info("[{}] No points used, skip freezing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "freeze"), "NO_POINTS");
                return;
            }
            
            // 构建冻结命令
            PointsOperationCommand command = PointsOperationCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .points(pointsUsed)
                    .bizType("ORDER_PAY")
                    .bizId(String.valueOf(orderId))
                    .idempotencyKey(buildIdempotencyKey(orderId, "freeze"))
                    .remark("订单结算冻结积分")
                    .build();
            
            // 调用 Facade 冻结积分
            PointsOperationResult result = pointsAssetFacade.freezePoints(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Points frozen successfully: eventId={}, orderId={}, points={}", 
                    CONSUMER_NAME, eventId, orderId, pointsUsed);
            
        } catch (Exception e) {
            log.error("[{}] Failed to freeze points: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单支付成功事件：提交扣减积分 + 赚取积分
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
            Integer pointsUsed = getIntegerField(event.getPayload().get("pointsUsed"));
            Integer pointsEarned = getIntegerField(event.getPayload().get("pointsEarned"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            // 1. 提交扣减积分（如果使用了积分）
            if (pointsUsed != null && pointsUsed > 0) {
                PointsOperationCommand commitCommand = PointsOperationCommand.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .points(pointsUsed)
                        .bizType("ORDER_PAY")
                        .bizId(String.valueOf(orderId))
                        .idempotencyKey(buildIdempotencyKey(orderId, "commit"))
                        .remark("订单支付扣减积分")
                        .build();
                
                PointsOperationResult commitResult = pointsAssetFacade.commitPoints(commitCommand);
                log.info("[{}] Points committed successfully: eventId={}, orderId={}, points={}", 
                        CONSUMER_NAME, eventId, orderId, pointsUsed);
            }
            
            // 2. 赚取积分（如果有积分奖励）
            if (pointsEarned != null && pointsEarned > 0) {
                PointsOperationCommand earnCommand = PointsOperationCommand.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .points(pointsEarned)
                        .bizType("ORDER_EARN")
                        .bizId(String.valueOf(orderId))
                        .idempotencyKey(buildIdempotencyKey(orderId, "earn"))
                        .remark("订单支付赚取积分")
                        .build();
                
                PointsOperationResult earnResult = pointsAssetFacade.commitPoints(earnCommand);
                log.info("[{}] Points earned successfully: eventId={}, orderId={}, points={}", 
                        CONSUMER_NAME, eventId, orderId, pointsEarned);
            }
            
            // 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, buildIdempotencyKey(orderId, "paid"), 
                    String.format("used=%s, earned=%s", pointsUsed, pointsEarned));
            
        } catch (Exception e) {
            log.error("[{}] Failed to process points on order paid: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单取消事件：释放积分冻结
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
            Integer pointsUsed = getIntegerField(event.getPayload().get("pointsUsed"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            if (pointsUsed == null || pointsUsed <= 0) {
                log.info("[{}] No points used, skip releasing: eventId={}, orderId={}", 
                        CONSUMER_NAME, eventId, orderId);
                consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                        tenantId, buildIdempotencyKey(orderId, "release"), "NO_POINTS");
                return;
            }
            
            // 构建释放命令
            PointsOperationCommand command = PointsOperationCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .points(pointsUsed)
                    .bizType("ORDER_PAY")
                    .bizId(String.valueOf(orderId))
                    .idempotencyKey(buildIdempotencyKey(orderId, "release"))
                    .remark("订单取消释放积分")
                    .build();
            
            // 调用 Facade 释放积分
            PointsOperationResult result = pointsAssetFacade.releasePoints(command);
            
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, command.getIdempotencyKey(), result);
            
            log.info("[{}] Points released successfully: eventId={}, orderId={}, points={}", 
                    CONSUMER_NAME, eventId, orderId, pointsUsed);
            
        } catch (Exception e) {
            log.error("[{}] Failed to release points: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 监听订单退款成功事件：回退积分
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
            Integer pointsUsed = getIntegerField(event.getPayload().get("pointsUsed"));
            Integer pointsEarned = getIntegerField(event.getPayload().get("pointsEarned"));
            Long tenantId = event.getTenantId();
            Long userId = event.getUserId();
            
            // 1. 回退已扣减的积分
            if (pointsUsed != null && pointsUsed > 0) {
                PointsOperationCommand revertUsedCommand = PointsOperationCommand.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .points(pointsUsed)
                        .bizType("ORDER_REFUND")
                        .bizId(String.valueOf(refundId))
                        .idempotencyKey(buildIdempotencyKey(refundId, "revert_used"))
                        .remark("订单退款返还积分")
                        .build();
                
                pointsAssetFacade.revertPoints(revertUsedCommand);
                log.info("[{}] Points reverted (used): eventId={}, orderId={}, refundId={}, points={}", 
                        CONSUMER_NAME, eventId, orderId, refundId, pointsUsed);
            }
            
            // 2. 回退已赚取的积分（扣减）
            if (pointsEarned != null && pointsEarned > 0) {
                PointsOperationCommand revertEarnedCommand = PointsOperationCommand.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .points(pointsEarned)
                        .bizType("ORDER_REFUND")
                        .bizId(String.valueOf(refundId))
                        .idempotencyKey(buildIdempotencyKey(refundId, "revert_earned"))
                        .remark("订单退款扣减赚取的积分")
                        .build();
                
                pointsAssetFacade.revertPoints(revertEarnedCommand);
                log.info("[{}] Points reverted (earned): eventId={}, orderId={}, refundId={}, points={}", 
                        CONSUMER_NAME, eventId, orderId, refundId, pointsEarned);
            }
            
            // 记录消费成功
            consumeLogService.recordSuccess(CONSUMER_NAME, eventId, event.getEventType(), 
                    tenantId, buildIdempotencyKey(refundId, "refunded"), 
                    String.format("revert_used=%s, revert_earned=%s", pointsUsed, pointsEarned));
            
        } catch (Exception e) {
            log.error("[{}] Failed to revert points: eventId={}, error={}", 
                    CONSUMER_NAME, eventId, e.getMessage(), e);
            consumeLogService.recordFailure(CONSUMER_NAME, eventId, event.getEventType(), 
                    event.getTenantId(), null, e.getMessage());
            throw e;
        }
    }
    
    private String buildIdempotencyKey(Long bizId, String action) {
        return String.format("points:%s:%s", bizId, action);
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
    
    private Integer getIntegerField(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
