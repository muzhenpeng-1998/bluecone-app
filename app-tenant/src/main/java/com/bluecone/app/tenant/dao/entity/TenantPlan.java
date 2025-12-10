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

    // 主键 ID，自增
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 套餐名称
    @Schema(description = "套餐名称")
    private String planName;

    // 套餐价格（按月或按年）
    @Schema(description = "价格（按月或按年）")
    private BigDecimal price;

    // 功能点配置，如最大门店数、是否允许多仓等
    @Schema(description = "功能点配置（如最大门店数、是否允许多仓等）")
    private String features;

    // 套餐状态：1启用，0禁用
    @Schema(description = "1启用，0禁用")
    private Byte status;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

}
