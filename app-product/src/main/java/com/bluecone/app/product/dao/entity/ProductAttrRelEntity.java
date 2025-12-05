package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品与属性组绑定实体，对应表 {@code bc_product_attr_rel}。
 * <p>
 * 定义商品启用哪些属性组及是否必选，可覆盖属性组默认的选择类型，用于下单时控制必选项与展示顺序。
 */
@Data
@TableName("bc_product_attr_rel")
public class ProductAttrRelEntity implements Serializable {

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
     * 属性组ID。
     * 对应表字段：attr_group_id。
     */
    @TableField("attr_group_id")
    private Long attrGroupId;

    /**
     * 在该商品下是否必选：1是，0否。
     * 对应表字段：required。
     */
    @TableField("required")
    private Boolean required;

    /**
     * 可选覆盖属性组默认的选择类型，NULL 表示沿用属性组配置。
     * 对应表字段：select_type。
     */
    @TableField("select_type")
    private Integer selectType;

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
