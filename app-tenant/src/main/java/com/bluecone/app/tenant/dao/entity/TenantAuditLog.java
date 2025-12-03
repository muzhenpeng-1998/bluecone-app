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
 * 租户审计日志
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantAuditLog", description = "租户审计日志")
public class TenantAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "操作者用户ID（平台管理员/店铺管理员）")
    private Long operatorId;

    @Schema(description = "操作行为：修改资料/套餐变更/认证审核等")
    private String action;

    @Schema(description = "操作详情")
    private String detail;

    private LocalDateTime createdAt;

}
