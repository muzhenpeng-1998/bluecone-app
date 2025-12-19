package com.bluecone.app.infra.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台角色实体
 */
@Data
@TableName("bc_admin_role")
public class AdminRoleEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("tenant_id")
    private Long tenantId;
    
    @TableField("role_code")
    private String roleCode;
    
    @TableField("role_name")
    private String roleName;
    
    @TableField("role_type")
    private String roleType;
    
    @TableField("description")
    private String description;
    
    @TableField("status")
    private String status;
    
    @TableField("ext_json")
    private String extJson;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("created_by")
    private Long createdBy;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("updated_by")
    private Long updatedBy;
}
