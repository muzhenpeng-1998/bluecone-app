package com.bluecone.app.member.domain.model;

import com.bluecone.app.member.domain.enums.PointsDirection;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水实体（账本记录）
 * 记录所有积分变动，保证可追溯、可对账
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
public class PointsLedger {
    
    /**
     * 流水ID（内部主键）
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
     * 变动方向
     */
    private PointsDirection direction;
    
    /**
     * 变动积分值（正数）
     */
    private Long deltaPoints;
    
    /**
     * 变动前可用积分
     */
    private Long beforeAvailable;
    
    /**
     * 变动前冻结积分
     */
    private Long beforeFrozen;
    
    /**
     * 变动后可用积分
     */
    private Long afterAvailable;
    
    /**
     * 变动后冻结积分
     */
    private Long afterFrozen;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private String bizId;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 备注说明
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建人
     */
    private Long createdBy;
    
    /**
     * 创建流水记录
     */
    public static PointsLedger create(
            Long id,
            Long tenantId, 
            Long memberId,
            PointsDirection direction,
            Long deltaPoints,
            Long beforeAvailable,
            Long beforeFrozen,
            Long afterAvailable,
            Long afterFrozen,
            String bizType,
            String bizId,
            String idempotencyKey,
            String remark) {
        
        PointsLedger ledger = new PointsLedger();
        ledger.id = id;
        ledger.tenantId = tenantId;
        ledger.memberId = memberId;
        ledger.direction = direction;
        ledger.deltaPoints = deltaPoints;
        ledger.beforeAvailable = beforeAvailable;
        ledger.beforeFrozen = beforeFrozen;
        ledger.afterAvailable = afterAvailable;
        ledger.afterFrozen = afterFrozen;
        ledger.bizType = bizType;
        ledger.bizId = bizId;
        ledger.idempotencyKey = idempotencyKey;
        ledger.remark = remark;
        ledger.createdAt = LocalDateTime.now();
        return ledger;
    }
    
    /**
     * 验证流水记录的完整性
     */
    public void validate() {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("会员ID不能为空");
        }
        if (direction == null) {
            throw new IllegalArgumentException("变动方向不能为空");
        }
        if (deltaPoints == null || deltaPoints <= 0) {
            throw new IllegalArgumentException("变动积分值必须大于0");
        }
        if (bizType == null || bizType.trim().isEmpty()) {
            throw new IllegalArgumentException("业务类型不能为空");
        }
        if (bizId == null || bizId.trim().isEmpty()) {
            throw new IllegalArgumentException("业务ID不能为空");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
    }
}
