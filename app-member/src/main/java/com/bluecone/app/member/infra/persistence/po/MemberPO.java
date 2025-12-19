package com.bluecone.app.member.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员表 PO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
@TableName("bc_member")
public class MemberPO {
    
    /**
     * 会员ID（内部主键）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;
    
    /**
     * 平台用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 会员编号
     */
    @TableField("member_no")
    private String memberNo;
    
    /**
     * 会员状态
     */
    @TableField("status")
    private String status;
    
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
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 更新人
     */
    @TableField("updated_by")
    private Long updatedBy;
}
