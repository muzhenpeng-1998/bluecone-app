package com.bluecone.app.promo.domain.repository;

import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.domain.model.Coupon;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券领域仓储接口
 */
public interface CouponRepository {

    /**
     * 根据ID查询优惠券
     */
    Coupon findById(Long tenantId, Long couponId);

    /**
     * 根据券码查询优惠券
     */
    Coupon findByCouponCode(String couponCode);

    /**
     * 查询用户的优惠券列表
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param statusList 状态列表（可为null表示查询所有状态）
     * @param validTime 有效期校验时间（可为null表示不校验有效期）
     * @return 优惠券列表
     */
    List<Coupon> findUserCoupons(Long tenantId, Long userId, List<CouponStatus> statusList, LocalDateTime validTime);

    /**
     * 保存优惠券
     */
    void save(Coupon coupon);

    /**
     * 更新优惠券
     * 
     * @return 更新的行数
     */
    int update(Coupon coupon);

    /**
     * 更新优惠券状态（带状态校验，防止并发问题）
     * 
     * @param couponId 优惠券ID
     * @param expectedStatus 期望的当前状态
     * @param newStatus 新状态
     * @param orderId 订单ID（可选）
     * @param lockTime 锁定时间（可选）
     * @param useTime 使用时间（可选）
     * @return 更新的行数（0表示状态不匹配或券不存在）
     */
    int updateStatus(Long couponId, CouponStatus expectedStatus, CouponStatus newStatus, 
                     Long orderId, LocalDateTime lockTime, LocalDateTime useTime);

    /**
     * 批量查询优惠券
     */
    List<Coupon> findByIds(Long tenantId, List<Long> couponIds);
}
