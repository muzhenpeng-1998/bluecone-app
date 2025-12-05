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
 * 通用属性项实体，对应表 {@code bc_product_attr_option}。
 * <p>
 * 属性项描述具体的可选值（如“无糖”“少糖”），可设置值编码和价格增减，配合属性组形成可复用的属性库。
 */
@Data
@TableName("bc_product_attr_option")
public class BcProductAttrOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性项ID，自增主键。
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
     * 属性组ID。
     * 对应表字段：attr_group_id。
     */
    @TableField("attr_group_id")
    private Long attrGroupId;

    /**
     * 属性项名称，如“无糖”“少糖”。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 值编码，可用于埋点或规则引擎。
     * 对应表字段：value_code。
     */
    @TableField("value_code")
    private String valueCode;

    /**
     * 价格增减，可能为 0 表示不加价。
     * 对应表字段：price_delta。
     */
    @TableField("price_delta")
    private BigDecimal priceDelta;

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
