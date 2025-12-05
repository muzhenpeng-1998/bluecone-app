package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品与分类关联实体，对应表 {@code bc_product_category_rel}。
 * <p>
 * 用于维护商品（SPU）与分类的多对多关系，支持在分类下排序及状态控制，常见于菜单分组、运营分组等场景。
 */
@Data
@TableName("bc_product_category_rel")
public class BcProductCategoryRel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增。
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
     * 分类ID。
     * 对应表字段：category_id。
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品ID（SPU）。
     * 对应表字段：product_id。
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 在分类下的排序值，越大越靠前。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：1有效，0无效。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

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
