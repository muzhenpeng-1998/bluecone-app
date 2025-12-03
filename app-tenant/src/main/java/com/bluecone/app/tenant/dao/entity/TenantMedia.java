package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 租户相关图片/素材资源
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantMedia", description = "租户相关图片/素材资源")
public class TenantMedia implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "素材类型：avatar、logo、banner、license、cover")
    private String mediaType;

    @Schema(description = "素材URL（OSS/COS/S3）")
    private String url;

    @Schema(description = "备注")
    private String description;

    private LocalDateTime createdAt;

}
