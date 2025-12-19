package com.bluecone.app.infra.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台权限实体
 */
@Data
@TableName("bc_admin_permission")
public class AdminPermissionEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("permission_code")
    private String permissionCode;
    
    @TableField("permission_name")
    private String permissionName;
    
    @TableField("resource_type")
    private String resourceType;
    
    @TableField("action")
    private String action;
    
    @TableField("description")
    private String description;
    
    @TableField("parent_id")
    private Long parentId;
    
    @TableField("sort_order")
    private Integer sortOrder;
    
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
