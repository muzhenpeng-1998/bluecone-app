package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品分类/菜单分组实体，对应表 {@code bc_product_category}。
 * <p>
 * 该表承载商品在菜单中的分组与层级结构，可用于多级分类（一级/二级等），支持区分商品/服务/场馆等类型。
 * 常用于构建门店菜单树、前端分类导航与运营分组。
 */
@Data
@TableName("bc_product_category")
public class ProductCategoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID，自增主键。
     * 对应表字段：id。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户隔离。
     * 对应表字段：tenant_id。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 父分类ID，0 表示顶级分类。
     * 对应表字段：parent_id。
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 分类类型：1商品菜单，2服务菜单，3场馆等。
     * 对应表字段：type。
     */
    @TableField("type")
    private Integer type;

    /**
     * 层级：1一级分类，2二级分类等。
     * 对应表字段：level。
     */
    @TableField("level")
    private Integer level;

    /**
     * 分类图标 URL，用于前端展示。
     * 对应表字段：icon。
     */
    @TableField("icon")
    private String icon;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，同级内越大越靠前。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 备注。
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

    /**
     * 创建人ID。
     * 对应表字段：created_by。
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 更新人ID。
     * 对应表字段：updated_by。
     */
    @TableField("updated_by")
    private Long updatedBy;
}
