package com.bluecone.app.member.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分账户聚合根
 * 采用乐观锁保证并发安全
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
public class PointsAccount {
    
    /**
     * 账户ID（内部主键）
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 会员ID
     */
    private Long memberId;
    
    /**
     * 可用积分
     */
    private Long availablePoints;
    
    /**
     * 冻结积分
     */
    private Long frozenPoints;
    
    /**
     * 乐观锁版本号
     */
    private Integer version;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建新账户
     */
    public static PointsAccount create(Long id, Long tenantId, Long memberId) {
        PointsAccount account = new PointsAccount();
        account.id = id;
        account.tenantId = tenantId;
        account.memberId = memberId;
        account.availablePoints = 0L;
        account.frozenPoints = 0L;
        account.version = 0;
        account.createdAt = LocalDateTime.now();
        account.updatedAt = LocalDateTime.now();
        return account;
    }
    
    /**
     * 增加可用积分
     */
    public void increaseAvailable(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
        }
        this.availablePoints += points;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 减少可用积分
     */
    public void decreaseAvailable(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
        }
        if (this.availablePoints < points) {
            throw new IllegalStateException("可用积分不足");
        }
        this.availablePoints -= points;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 冻结积分（从可用转到冻结）
     */
    public void freeze(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
        }
        if (this.availablePoints < points) {
            throw new IllegalStateException("可用积分不足，无法冻结");
        }
        this.availablePoints -= points;
        this.frozenPoints += points;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 释放冻结积分（从冻结转回可用）
     */
    public void release(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
        }
        if (this.frozenPoints < points) {
            throw new IllegalStateException("冻结积分不足");
        }
        this.frozenPoints -= points;
        this.availablePoints += points;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 提交扣减冻结积分（支付成功后从冻结中扣除）
     */
    public void commitSpend(Long points) {
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
        }
        if (this.frozenPoints < points) {
            throw new IllegalStateException("冻结积分不足");
        }
        this.frozenPoints -= points;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 获取总积分
     */
    public Long getTotalPoints() {
        return availablePoints + frozenPoints;
    }
}
