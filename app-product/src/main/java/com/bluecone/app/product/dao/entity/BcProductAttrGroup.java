package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 通用属性组实体，对应表 {@code bc_product_attr_group}。
 * <p>
 * 属性组在租户维度维护，可复用到多个商品，用于口味、制作方式、展示标签等属性定义，支持单选/多选、是否必选及最大可选数量。
 */
@Data
@TableName("bc_product_attr_group")
public class BcProductAttrGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 属性组ID，自增主键。
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
     * 属性组名称，例如“甜度”“做法”“忌口”。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 作用范围：1口味，2制作，3展示标签等。
     * 对应表字段：scope。
     */
    @TableField("scope")
    private Integer scope;

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
     * 最大可选数量，0 表示不限制或按选择类型默认。
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
     * 排序值。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 备注说明。
     * 对应表字段：remark。
     */
    @TableField("remark")
    private String remark;

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
