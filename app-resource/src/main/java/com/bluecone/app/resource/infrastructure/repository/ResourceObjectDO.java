package com.bluecone.app.resource.infrastructure.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源物理对象表映射 BC_RES_OBJECT。
 */
@Data
@TableName("bc_res_object")
public class ResourceObjectDO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("profile_code")
    private String profileCode;

    @TableField("storage_provider")
    private String storageProvider;

    @TableField("storage_key")
    private String storageKey;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("content_type")
    private String contentType;

    @TableField("file_ext")
    private String fileExt;

    @TableField("hash_sha256")
    private String hashSha256;

    @TableField("access_level")
    private Integer accessLevel;

    private Integer status;

    @TableField("ext_json")
    private String extJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
