package com.bluecone.app.infra.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联实体
 */
@Data
@TableName("bc_admin_user_role")
public class AdminUserRoleEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("tenant_id")
    private Long tenantId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("role_id")
    private Long roleId;
    
    @TableField("scope_type")
    private String scopeType;
    
    @TableField("scope_id")
    private Long scopeId;
    
    @TableField("valid_from")
    private LocalDateTime validFrom;
    
    @TableField("valid_until")
    private LocalDateTime validUntil;
    
    @TableField("status")
    private String status;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("created_by")
    private Long createdBy;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("updated_by")
    private Long updatedBy;
}
