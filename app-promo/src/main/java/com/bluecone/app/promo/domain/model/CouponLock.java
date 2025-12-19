package com.bluecone.app.promo.domain.model;

import com.bluecone.app.promo.api.enums.LockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券锁定记录领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponLock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long couponId;
    private Long userId;
    private Long orderId;
    private String idempotencyKey;
    private LockStatus lockStatus;
    private LocalDateTime lockTime;
    private LocalDateTime releaseTime;
    private LocalDateTime commitTime;
    private LocalDateTime expireTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 是否已锁定
     */
    public boolean isLocked() {
        return lockStatus == LockStatus.LOCKED;
    }

    /**
     * 是否已释放
     */
    public boolean isReleased() {
        return lockStatus == LockStatus.RELEASED;
    }

    /**
     * 是否已提交
     */
    public boolean isCommitted() {
        return lockStatus == LockStatus.COMMITTED;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expireTime);
    }

    /**
     * 释放锁定
     */
    public void release(LocalDateTime releaseTime) {
        if (isReleased() || isCommitted()) {
            // 幂等：已经释放或提交，直接返回
            return;
        }
        this.lockStatus = LockStatus.RELEASED;
        this.releaseTime = releaseTime;
    }

    /**
     * 提交锁定（核销）
     */
    public void commit(LocalDateTime commitTime) {
        if (isCommitted()) {
            // 幂等：已经提交，直接返回
            return;
        }
        if (!isLocked()) {
            throw new IllegalStateException("锁定状态不是LOCKED，无法提交");
        }
        this.lockStatus = LockStatus.COMMITTED;
        this.commitTime = commitTime;
    }
}
