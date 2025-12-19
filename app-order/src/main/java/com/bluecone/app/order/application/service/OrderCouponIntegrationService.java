package com.bluecone.app.order.application.service;

import com.bluecone.app.promo.api.dto.CouponCommitCommand;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.dto.CouponReleaseCommand;
import com.bluecone.app.promo.api.facade.CouponLockFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单优惠券集成服务
 * 
 * <p>职责：
 * <ul>
 *   <li>订单提交时锁定优惠券</li>
 *   <li>订单取消/超时时释放优惠券</li>
 *   <li>订单支付成功时核销优惠券</li>
 *   <li>生成幂等键确保操作幂等</li>
 * </ul>
 * 
 * <p>幂等键规则：
 * <ul>
 *   <li>锁定/释放/核销使用相同的幂等键：orderId:couponId</li>
 *   <li>确保同一订单的同一优惠券的操作序列幂等</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCouponIntegrationService {

    private final CouponLockFacade couponLockFacade;

    /**
     * 锁定优惠券（订单提交时调用）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @param orderId 订单ID
     * @param orderAmount 订单金额
     * @return 锁定结果
     */
    public CouponLockResult lockCoupon(Long tenantId, Long userId, Long couponId, 
                                       Long orderId, BigDecimal orderAmount) {
        if (couponId == null) {
            log.debug("订单未使用优惠券，跳过锁定: orderId={}", orderId);
            return null;
        }

        try {
            String idempotencyKey = buildIdempotencyKey(orderId, couponId);
            
            CouponLockCommand command = new CouponLockCommand();
            command.setTenantId(tenantId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setOrderAmount(orderAmount);
            command.setIdempotencyKey(idempotencyKey);
            command.setLockExpireMinutes(30); // 默认30分钟过期

            CouponLockResult result = couponLockFacade.lock(command);
            
            if (result != null && result.getSuccess()) {
                log.info("优惠券锁定成功: orderId={}, couponId={}, discountAmount={}", 
                        orderId, couponId, result.getDiscountAmount());
            } else {
                log.warn("优惠券锁定失败: orderId={}, couponId={}, message={}", 
                        orderId, couponId, result != null ? result.getMessage() : "unknown");
            }
            
            return result;
        } catch (Exception e) {
            log.error("锁定优惠券异常: orderId={}, couponId={}", orderId, couponId, e);
            // 不抛出异常，允许订单继续（优惠券锁定失败不应阻止下单）
            return null;
        }
    }

    /**
     * 释放优惠券（订单取消/超时时调用）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @param orderId 订单ID
     */
    public void releaseCoupon(Long tenantId, Long userId, Long couponId, Long orderId) {
        if (couponId == null) {
            log.debug("订单未使用优惠券，跳过释放: orderId={}", orderId);
            return;
        }

        try {
            String idempotencyKey = buildIdempotencyKey(orderId, couponId);
            
            CouponReleaseCommand command = new CouponReleaseCommand();
            command.setTenantId(tenantId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setIdempotencyKey(idempotencyKey);

            couponLockFacade.release(command);
            
            log.info("优惠券释放成功: orderId={}, couponId={}", orderId, couponId);
        } catch (Exception e) {
            log.error("释放优惠券异常: orderId={}, couponId={}", orderId, couponId, e);
            // 不抛出异常，继续执行后续逻辑
        }
    }

    /**
     * 核销优惠券（订单支付成功时调用）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @param orderId 订单ID
     * @param originalAmount 原始订单金额
     * @param discountAmount 优惠金额
     * @param finalAmount 最终金额
     */
    public void commitCoupon(Long tenantId, Long userId, Long couponId, Long orderId,
                            BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal finalAmount) {
        if (couponId == null) {
            log.debug("订单未使用优惠券，跳过核销: orderId={}", orderId);
            return;
        }

        try {
            String idempotencyKey = buildIdempotencyKey(orderId, couponId);
            
            CouponCommitCommand command = new CouponCommitCommand();
            command.setTenantId(tenantId);
            command.setUserId(userId);
            command.setCouponId(couponId);
            command.setOrderId(orderId);
            command.setOriginalAmount(originalAmount);
            command.setDiscountAmount(discountAmount);
            command.setFinalAmount(finalAmount);
            command.setIdempotencyKey(idempotencyKey);

            couponLockFacade.commit(command);
            
            log.info("优惠券核销成功: orderId={}, couponId={}, discountAmount={}", 
                    orderId, couponId, discountAmount);
        } catch (Exception e) {
            log.error("核销优惠券异常: orderId={}, couponId={}", orderId, couponId, e);
            // 不抛出异常，但应该记录告警（核销失败可能需要人工介入）
            // TODO: 发送告警通知
        }
    }

    /**
     * 构建幂等键
     * 
     * <p>规则：orderId:couponId
     * <p>说明：同一订单的同一优惠券的所有操作（锁定/释放/核销）使用相同的幂等键
     */
    private String buildIdempotencyKey(Long orderId, Long couponId) {
        return String.format("ORDER:%d:COUPON:%d", orderId, couponId);
    }
}
