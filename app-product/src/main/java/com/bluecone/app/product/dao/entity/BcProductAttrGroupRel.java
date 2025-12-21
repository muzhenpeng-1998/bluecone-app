package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品绑定属性组实体，对应表 {@code bc_product_attr_group_rel}。
 * <p>
 * 定义商品启用哪些属性组及组级规则（是否必选、最小/最大选择数量、排序、定时展示），
 * 用于控制属性组在商品下的展示与选择规则。
 * <p>
 * 与 {@code bc_product_attr_rel} 的区别：
 * <ul>
 *   <li>本表（group_rel）：组级规则，控制整个属性组的必选性、选择范围、排序、定时展示</li>
 *   <li>attr_rel：选项级覆盖，控制单个属性选项的禁用、排序、加价覆盖</li>
 * </ul>
 */
@Data
@TableName("bc_product_attr_group_rel")
public class BcProductAttrGroupRel implements Serializable {

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
     * 最小选择数量，0 表示不限制。
     * 对应表字段：min_select。
     */
    @TableField("min_select")
    private Integer minSelect;

    /**
     * 最大选择数量，NULL 表示不限制。
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
     * 排序值，用于控制属性组在商品下的展示顺序。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 展示开始时间，NULL 表示立即生效。
     * 对应表字段：display_start_at。
     */
    @TableField("display_start_at")
    private LocalDateTime displayStartAt;

    /**
     * 展示结束时间，NULL 表示永久有效。
     * 对应表字段：display_end_at。
     */
    @TableField("display_end_at")
    private LocalDateTime displayEndAt;

    /**
     * 逻辑删除：0未删除，1已删除。
     * 对应表字段：deleted。
     */
    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

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

