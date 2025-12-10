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

    // 主键 ID，自增
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户 ID，关联 tenant.id
    @Schema(description = "租户ID")
    private Long tenantId;

    // 配置 key，例如 plan.id / plan.expireAt
    @Schema(description = "配置Key")
    private String keyName;

    // 配置值，以字符串形式存储
    @Schema(description = "配置值")
    private String keyValue;

    // 最后一次更新时间
    private LocalDateTime updatedAt;

}
