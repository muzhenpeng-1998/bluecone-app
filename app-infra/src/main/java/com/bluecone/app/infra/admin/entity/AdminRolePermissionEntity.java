package com.bluecone.app.infra.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-权限关联实体
 */
@Data
@TableName("bc_admin_role_permission")
public class AdminRolePermissionEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("tenant_id")
    private Long tenantId;
    
    @TableField("role_id")
    private Long roleId;
    
    @TableField("permission_id")
    private Long permissionId;
    
    @TableField("status")
    private String status;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("created_by")
    private Long createdBy;
}
