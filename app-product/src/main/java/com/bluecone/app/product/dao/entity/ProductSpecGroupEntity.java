package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品规格组实体，对应表 {@code bc_product_spec_group}。
 * <p>
 * 规格组定义了商品的规格维度（如“容量”“温度”），配置选择类型、是否必选及最大选择数量，
 * 用于构建商品的可选规格体系，供 SKU 组合与下单选择使用。
 */
@Data
@TableName("bc_product_spec_group")
public class ProductSpecGroupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规格组ID，自增主键。
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
     * 规格组名称，如“容量”。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 选择类型：1单选，2多选。
     * 对应表字段：select_type。
     */
    @TableField("select_type")
    private Integer selectType;

    /**
     * 是否必选：1是，0否。
     * 对应表字段：required。
     */
    @TableField("required")
    private Boolean required;

    /**
     * 允许最大选择数量，多选时生效。
     * 对应表字段：max_select。
     */
    @TableField("max_select")
    private Integer maxSelect;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，用于规格组展示顺序。
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
