package com.bluecone.app.member.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分账户表 PO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@TableName("bc_points_account")
public class PointsAccountPO {
    
    /**
     * 账户ID（内部主键）
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
     * 可用积分
     */
    @TableField("available_points")
    private Long availablePoints;
    
    /**
     * 冻结积分
     */
    @TableField("frozen_points")
    private Long frozenPoints;
    
    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
