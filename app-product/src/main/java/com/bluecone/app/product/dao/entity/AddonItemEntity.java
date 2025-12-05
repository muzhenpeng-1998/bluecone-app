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
 * 小料项实体，对应表 {@code bc_addon_item}。
 * <p>
 * 描述小料组下的具体小料（如“珍珠”“燕麦奶”），可配置价格、免费份数与最大可选数量，用于下单加料计价。
 */
@Data
@TableName("bc_addon_item")
public class AddonItemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 小料项ID，自增主键。
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
     * 所属小料组ID。
     * 对应表字段：group_id。
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 小料名称。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 单份小料价格（不计价小料可为 0）。
     * 对应表字段：price。
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 单商品最多可选数量，支持小数精度以兼容重量型加料。
     * 对应表字段：max_quantity。
     */
    @TableField("max_quantity")
    private BigDecimal maxQuantity;

    /**
     * 前 N 份免费，从 N+1 份开始计费。
     * 对应表字段：free_limit。
     */
    @TableField("free_limit")
    private BigDecimal freeLimit;

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
