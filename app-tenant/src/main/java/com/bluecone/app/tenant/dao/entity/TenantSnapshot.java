package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 租户聚合快照
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantSnapshot", description = "租户聚合快照")
public class TenantSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "tenant_id", type = IdType.AUTO)
    private Long tenantId;

    @Schema(description = "TenantSnapshot 的序列化数据")
    private String snapshot;

    private Long version;

    private LocalDateTime updatedAt;

}
