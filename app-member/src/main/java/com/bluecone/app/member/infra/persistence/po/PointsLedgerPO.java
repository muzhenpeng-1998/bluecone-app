package com.bluecone.app.member.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水表 PO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@TableName("bc_points_ledger")
public class PointsLedgerPO {
    
    /**
     * 流水ID（内部主键）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;
    
    /**
     * 会员ID
     */
    @TableField("member_id")
    private Long memberId;
    
    /**
     * 变动方向
     */
    @TableField("direction")
    private String direction;
    
    /**
     * 变动积分值
     */
    @TableField("delta_points")
    private Long deltaPoints;
    
    /**
     * 变动前可用积分
     */
    @TableField("before_available")
    private Long beforeAvailable;
    
    /**
     * 变动前冻结积分
     */
    @TableField("before_frozen")
    private Long beforeFrozen;
    
    /**
     * 变动后可用积分
     */
    @TableField("after_available")
    private Long afterAvailable;
    
    /**
     * 变动后冻结积分
     */
    @TableField("after_frozen")
    private Long afterFrozen;
    
    /**
     * 业务类型
     */
    @TableField("biz_type")
    private String bizType;
    
    /**
     * 业务ID
     */
    @TableField("biz_id")
    private String bizId;
    
    /**
     * 幂等键
     */
    @TableField("idempotency_key")
    private String idempotencyKey;
    
    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 创建人
     */
    @TableField("created_by")
    private Long createdBy;
}
