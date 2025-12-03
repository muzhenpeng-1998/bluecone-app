package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

/**
 * 租户套餐定义
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
@Schema(name = "TenantPlan", description = "租户套餐定义")
public class TenantPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "套餐名称")
    private String planName;

    @Schema(description = "价格（按月或按年）")
    private BigDecimal price;

    @Schema(description = "功能点配置（如最大门店数、是否允许多仓等）")
    private String features;

    @Schema(description = "1启用，0禁用")
    private Byte status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
