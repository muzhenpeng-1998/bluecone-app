package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品与小料组绑定实体，对应表 {@code bc_product_addon_rel}。
 * <p>
 * 指定商品可使用哪些小料组及其选择规则，如是否必须选择、总可选上限和排序，支撑加料配置与下单校验。
 */
@Data
@TableName("bc_product_addon_rel")
public class ProductAddonRelEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增。
     * 对应表字段：id。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID。
     * 对应表字段：tenant_id。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 商品ID（SPU）。
     * 对应表字段：product_id。
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 小料组ID。
     * 对应表字段：addon_group_id。
     */
    @TableField("addon_group_id")
    private Long addonGroupId;

    /**
     * 是否必须选择小料：1是，0否。
     * 对应表字段：required。
     */
    @TableField("required")
    private Boolean required;

    /**
     * 该小料组在本商品下的总可选上限，0 表示不限制。
     * 对应表字段：max_total_quantity。
     */
    @TableField("max_total_quantity")
    private BigDecimal maxTotalQuantity;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间。
     * 对应表字段：created_at。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间，自动更新。
     * 对应表字段：updated_at。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
