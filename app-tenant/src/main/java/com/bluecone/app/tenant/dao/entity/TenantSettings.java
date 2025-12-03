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
 * 租户配置项（KV 模型）
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantSettings", description = "租户配置项（KV 模型）")
public class TenantSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "配置Key")
    private String keyName;

    @Schema(description = "配置值")
    private String keyValue;

    private LocalDateTime updatedAt;

}
