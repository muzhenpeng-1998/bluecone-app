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
 * 商品规格项实体，对应表 {@code bc_product_spec_option}。
 * <p>
 * 描述规格组选项（如“大杯”“热”），可配置价格增减与默认选中状态，配合规格组形成商品可选规格列表。
 */
@Data
@TableName("bc_product_spec_option")
public class ProductSpecOptionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规格项ID，自增主键。
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
     * 规格组ID。
     * 对应表字段：spec_group_id。
     */
    @TableField("spec_group_id")
    private Long specGroupId;

    /**
     * 规格项名称。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 价格增减，基于 SKU 基础价的浮动。
     * 对应表字段：price_delta。
     */
    @TableField("price_delta")
    private BigDecimal priceDelta;

    /**
     * 是否默认选中：1是，0否。
     * 对应表字段：is_default。
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，用于规格项展示顺序。
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
