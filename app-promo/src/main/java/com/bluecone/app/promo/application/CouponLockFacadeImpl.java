package com.bluecone.app.promo.application;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.dto.CouponCommitCommand;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.dto.CouponReleaseCommand;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.LockStatus;
import com.bluecone.app.promo.api.facade.CouponLockFacade;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponLock;
import com.bluecone.app.promo.domain.model.CouponRedemption;
import com.bluecone.app.promo.domain.repository.CouponLockRepository;
import com.bluecone.app.promo.domain.repository.CouponRedemptionRepository;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券锁定门面实现
 * 
 * <p>核心设计：
 * <ul>
 *   <li>幂等保证：通过 idempotencyKey 的唯一约束实现幂等，重复调用返回相同结果</li>
 *   <li>状态机：券状态 ISSUED → LOCKED → USED，通过数据库层面的状态校验防止并发问题</li>
 *   <li>锁定记录：独立的 lock 表记录锁定状态，券表状态同步更新</li>
 *   <li>并发安全：利用数据库唯一约束（idempotencyKey）和乐观更新（券状态更新时校验当前状态）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponLockFacadeImpl implements CouponLockFacade {

    private final CouponRepository couponRepository;
    private final CouponLockRepository couponLockRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final IdService idService;

    private static final int DEFAULT_LOCK_EXPIRE_MINUTES = 30;

    /**
     * 锁定优惠券
     * 
     * <p>幂等逻辑：
     * <ol>
     *   <li>先查询 lock 表，如果已存在则返回已有结果（幂等）</li>
     *   <li>校验券状态和有效性</li>
     *   <li>插入 lock 记录（幂等键唯一约束兜底）</li>
     *   <li>更新券状态为 LOCKED（带状态校验，防止并发）</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponLockResult lock(CouponLockCommand command) {
        // 1. 参数校验
        validateLockCommand(command);
        
        // 2. 幂等校验：如果已经存在锁定记录，直接返回
        CouponLock existingLock = couponLockRepository.findByIdempotencyKey(command.getIdempotencyKey());
        if (existingLock != null) {
            log.info("优惠券已锁定（幂等）: couponId={}, orderId={}, idempotencyKey={}", 
                    command.getCouponId(), command.getOrderId(), command.getIdempotencyKey());
            return buildLockResult(existingLock, true, "优惠券已锁定（幂等）");
        }
        
        // 3. 查询券信息并校验
        Coupon coupon = couponRepository.findById(command.getTenantId(), command.getCouponId());
        if (coupon == null) {
            return buildFailureResult("优惠券不存在");
        }
        
        if (!coupon.isUsable()) {
            return buildFailureResult("优惠券状态不可用，当前状态：" + coupon.getStatus());
        }
        
        // 4. 校验有效期
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isValid(now)) {
            return buildFailureResult("优惠券已过期或尚未生效");
        }
        
        // 5. 校验门槛
        if (!coupon.meetsThreshold(command.getOrderAmount())) {
            return buildFailureResult(String.format("订单金额未达到使用门槛（满%.2f元可用）", 
                    coupon.getMinOrderAmount()));
        }
        
        // 6. 计算优惠金额
        BigDecimal discountAmount = coupon.calculateDiscount(command.getOrderAmount());
        
        // 7. 创建锁定记录
        int lockExpireMinutes = command.getLockExpireMinutes() != null 
                ? command.getLockExpireMinutes() 
                : DEFAULT_LOCK_EXPIRE_MINUTES;
        
        LocalDateTime expireTime = now.plusMinutes(lockExpireMinutes);
        
        CouponLock lock = CouponLock.builder()
                .id(idService.nextLong(IdScope.COUPON_LOCK))
                .tenantId(command.getTenantId())
                .couponId(command.getCouponId())
                .userId(command.getUserId())
                .orderId(command.getOrderId())
                .idempotencyKey(command.getIdempotencyKey())
                .lockStatus(LockStatus.LOCKED)
                .lockTime(now)
                .expireTime(expireTime)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        // 8. 插入锁定记录（幂等键约束兜底）
        try {
            couponLockRepository.save(lock);
        } catch (Exception e) {
            // 如果插入失败（幂等键冲突），查询已有记录返回
            log.warn("插入锁定记录失败，可能是幂等键冲突: idempotencyKey={}", command.getIdempotencyKey(), e);
            CouponLock retryLock = couponLockRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (retryLock != null) {
                return buildLockResult(retryLock, true, "优惠券已锁定（幂等）");
            }
            throw new RuntimeException("锁定优惠券失败：" + e.getMessage(), e);
        }
        
        // 9. 更新券状态为 LOCKED（带状态校验，防止并发）
        int updated = couponRepository.updateStatus(
                command.getCouponId(),
                CouponStatus.ISSUED,  // 期望当前状态
                CouponStatus.LOCKED,  // 新状态
                command.getOrderId(), // 关联订单
                now,                  // 锁定时间
                null                  // 使用时间
        );
        
        if (updated == 0) {
            // 状态更新失败，说明券已被其他请求锁定或状态不对
            log.warn("更新券状态失败，券可能已被锁定: couponId={}, orderId={}", 
                    command.getCouponId(), command.getOrderId());
            return buildFailureResult("优惠券已被占用或状态不可用");
        }
        
        log.info("优惠券锁定成功: couponId={}, orderId={}, idempotencyKey={}, discountAmount={}", 
                command.getCouponId(), command.getOrderId(), command.getIdempotencyKey(), discountAmount);
        
        return buildLockResult(lock, discountAmount);
    }

    /**
     * 释放优惠券
     * 
     * <p>幂等逻辑：
     * <ol>
     *   <li>查询锁定记录</li>
     *   <li>如果已释放或已提交，直接返回（幂等）</li>
     *   <li>更新锁定记录状态为 RELEASED</li>
     *   <li>更新券状态回 ISSUED</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(CouponReleaseCommand command) {
        // 1. 参数校验
        validateReleaseCommand(command);
        
        // 2. 查询锁定记录
        CouponLock lock = couponLockRepository.findByIdempotencyKey(command.getIdempotencyKey());
        if (lock == null) {
            log.warn("锁定记录不存在，可能未锁定或已释放: idempotencyKey={}", command.getIdempotencyKey());
            return; // 幂等：不存在视为已释放
        }
        
        // 3. 幂等检查：如果已释放或已提交，直接返回
        if (lock.isReleased() || lock.isCommitted()) {
            log.info("优惠券已释放或已提交（幂等）: couponId={}, orderId={}, lockStatus={}", 
                    command.getCouponId(), command.getOrderId(), lock.getLockStatus());
            return;
        }
        
        // 4. 更新锁定记录状态
        LocalDateTime now = LocalDateTime.now();
        lock.release(now);
        couponLockRepository.update(lock);
        
        // 5. 更新券状态回 ISSUED
        int updated = couponRepository.updateStatus(
                command.getCouponId(),
                CouponStatus.LOCKED,  // 期望当前状态
                CouponStatus.ISSUED,  // 新状态
                null,                 // 清空订单ID
                null,                 // 清空锁定时间
                null                  // 清空使用时间
        );
        
        if (updated == 0) {
            log.warn("更新券状态失败，券可能已被使用或状态异常: couponId={}, orderId={}", 
                    command.getCouponId(), command.getOrderId());
            // 注意：这里不抛异常，因为释放操作应该幂等且容错
        }
        
        log.info("优惠券释放成功: couponId={}, orderId={}, idempotencyKey={}", 
                command.getCouponId(), command.getOrderId(), command.getIdempotencyKey());
    }

    /**
     * 提交核销优惠券
     * 
     * <p>幂等逻辑：
     * <ol>
     *   <li>查询核销记录，如果已存在则直接返回（幂等）</li>
     *   <li>查询锁定记录并校验</li>
     *   <li>插入核销记录（幂等键唯一约束兜底）</li>
     *   <li>更新锁定记录状态为 COMMITTED</li>
     *   <li>更新券状态为 USED</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commit(CouponCommitCommand command) {
        // 1. 参数校验
        validateCommitCommand(command);
        
        // 2. 幂等校验：如果已经存在核销记录，直接返回
        CouponRedemption existingRedemption = couponRedemptionRepository.findByIdempotencyKey(command.getIdempotencyKey());
        if (existingRedemption != null) {
            log.info("优惠券已核销（幂等）: couponId={}, orderId={}, idempotencyKey={}", 
                    command.getCouponId(), command.getOrderId(), command.getIdempotencyKey());
            return;
        }
        
        // 3. 查询锁定记录并校验
        CouponLock lock = couponLockRepository.findByIdempotencyKey(command.getIdempotencyKey());
        if (lock == null) {
            log.error("锁定记录不存在，无法核销: couponId={}, orderId={}, idempotencyKey={}", 
                    command.getCouponId(), command.getOrderId(), command.getIdempotencyKey());
            throw new IllegalStateException("优惠券未锁定，无法核销");
        }
        
        if (lock.isCommitted()) {
            log.info("优惠券已核销（幂等）: couponId={}, orderId={}", command.getCouponId(), command.getOrderId());
            return;
        }
        
        if (lock.isReleased()) {
            log.error("优惠券锁定已释放，无法核销: couponId={}, orderId={}", command.getCouponId(), command.getOrderId());
            throw new IllegalStateException("优惠券锁定已释放，无法核销");
        }
        
        // 4. 查询券信息
        Coupon coupon = couponRepository.findById(command.getTenantId(), command.getCouponId());
        if (coupon == null) {
            throw new IllegalStateException("优惠券不存在");
        }
        
        // 5. 创建核销记录
        LocalDateTime now = LocalDateTime.now();
        CouponRedemption redemption = CouponRedemption.builder()
                .id(idService.nextLong(IdScope.COUPON_REDEMPTION))
                .tenantId(command.getTenantId())
                .couponId(command.getCouponId())
                .templateId(coupon.getTemplateId())
                .userId(command.getUserId())
                .orderId(command.getOrderId())
                .idempotencyKey(command.getIdempotencyKey())
                .originalAmount(command.getOriginalAmount())
                .discountAmount(command.getDiscountAmount())
                .finalAmount(command.getFinalAmount())
                .redemptionTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        // 6. 插入核销记录（幂等键约束兜底）
        try {
            couponRedemptionRepository.save(redemption);
        } catch (Exception e) {
            // 如果插入失败（幂等键冲突），查询已有记录确认
            log.warn("插入核销记录失败，可能是幂等键冲突: idempotencyKey={}", command.getIdempotencyKey(), e);
            CouponRedemption retryRedemption = couponRedemptionRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (retryRedemption != null) {
                log.info("优惠券已核销（幂等）: couponId={}, orderId={}", command.getCouponId(), command.getOrderId());
                return;
            }
            throw new RuntimeException("核销优惠券失败：" + e.getMessage(), e);
        }
        
        // 7. 更新锁定记录状态
        lock.commit(now);
        couponLockRepository.update(lock);
        
        // 8. 更新券状态为 USED
        int updated = couponRepository.updateStatus(
                command.getCouponId(),
                CouponStatus.LOCKED,  // 期望当前状态
                CouponStatus.USED,    // 新状态
                command.getOrderId(), // 订单ID
                null,                 // 锁定时间（不更新）
                now                   // 使用时间
        );
        
        if (updated == 0) {
            log.error("更新券状态失败，券状态异常: couponId={}, orderId={}", 
                    command.getCouponId(), command.getOrderId());
            throw new IllegalStateException("优惠券状态异常，无法核销");
        }
        
        log.info("优惠券核销成功: couponId={}, orderId={}, idempotencyKey={}, discountAmount={}", 
                command.getCouponId(), command.getOrderId(), command.getIdempotencyKey(), command.getDiscountAmount());
    }

    // ==================== 校验方法 ====================

    private void validateLockCommand(CouponLockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("锁定命令不能为空");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (command.getCouponId() == null) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        if (command.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
        if (command.getOrderAmount() == null || command.getOrderAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单金额必须大于0");
        }
    }

    private void validateReleaseCommand(CouponReleaseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("释放命令不能为空");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (command.getCouponId() == null) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        if (command.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
    }

    private void validateCommitCommand(CouponCommitCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("核销命令不能为空");
        }
        if (command.getTenantId() == null) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (command.getCouponId() == null) {
            throw new IllegalArgumentException("优惠券ID不能为空");
        }
        if (command.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
        if (command.getOriginalAmount() == null || command.getOriginalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("原始金额必须大于0");
        }
        if (command.getDiscountAmount() == null || command.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("优惠金额不能为负数");
        }
        if (command.getFinalAmount() == null || command.getFinalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最终金额不能为负数");
        }
    }

    // ==================== 结果构建方法 ====================

    private CouponLockResult buildLockResult(CouponLock lock, BigDecimal discountAmount) {
        CouponLockResult result = new CouponLockResult();
        result.setSuccess(true);
        result.setMessage("锁定成功");
        result.setLockId(lock.getId());
        result.setCouponId(lock.getCouponId());
        result.setDiscountAmount(discountAmount);
        result.setLockTime(lock.getLockTime());
        result.setExpireTime(lock.getExpireTime());
        return result;
    }

    private CouponLockResult buildLockResult(CouponLock lock, boolean success, String message) {
        CouponLockResult result = new CouponLockResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setLockId(lock.getId());
        result.setCouponId(lock.getCouponId());
        result.setLockTime(lock.getLockTime());
        result.setExpireTime(lock.getExpireTime());
        return result;
    }

    private CouponLockResult buildFailureResult(String message) {
        CouponLockResult result = new CouponLockResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
