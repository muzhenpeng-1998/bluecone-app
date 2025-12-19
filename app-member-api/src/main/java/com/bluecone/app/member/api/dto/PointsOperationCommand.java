package com.bluecone.app.member.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分操作命令基类
 * 所有积分操作都必须携带幂等键和业务上下文
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsOperationCommand {
    
    /**
     * 租户ID（必填）
     */
    private Long tenantId;
    
    /**
     * 会员ID（必填）
     */
    private Long memberId;
    
    /**
     * 用户ID（必填，与 memberId 相同，为了兼容性保留两个字段）
     */
    private Long userId;
    
    /**
     * 积分变动值（正数，方向由 direction 决定）
     */
    private Integer points;
    
    /**
     * 业务类型（必填）
     * 例如：ORDER_PAY、ORDER_COMPLETE、REFUND、ADJUST
     */
    private String bizType;
    
    /**
     * 业务ID（必填）
     * 例如：订单ID、退款单ID
     */
    private String bizId;
    
    /**
     * 幂等键（必填，全局唯一）
     * 格式建议：{tenantId}:{bizType}:{bizId}:{operation}
     * 例如：123:ORDER_COMPLETE:ord_xxx:earn
     */
    private String idempotencyKey;
    
    /**
     * 备注说明（可选）
     */
    private String remark;
    
    /**
     * 验证命令参数是否完整
     */
    public void validate() {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        // 兼容 userId 和 memberId
        if (userId != null && memberId == null) {
            memberId = userId;
        } else if (memberId != null && userId == null) {
            userId = memberId;
        }
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("会员ID不能为空");
        }
        if (points == null || points <= 0) {
            throw new IllegalArgumentException("积分值必须大于0");
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
