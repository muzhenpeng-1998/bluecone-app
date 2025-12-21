package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品绑定属性选项实体，对应表 {@code bc_product_attr_rel}。
 * <p>
 * 定义商品启用哪些属性选项及其选项级覆盖规则（价格增减覆盖、排序、启用状态），
 * 用于控制属性选项在商品下的展示与计价规则。
 * <p>
 * 与 {@code bc_product_attr_group_rel} 的区别：
 * <ul>
 *   <li>group_rel：组级规则，控制整个属性组的必选性、选择范围、排序、定时展示</li>
 *   <li>本表（attr_rel）：选项级覆盖，控制单个属性选项的价格、排序、启用状态</li>
 * </ul>
 */
@Data
@TableName("bc_product_attr_rel")
public class BcProductAttrRel implements Serializable {

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
     * 属性选项ID。
     * 对应表字段：attr_option_id。
     */
    @TableField("attr_option_id")
    private Long attrOptionId;

    /**
     * 价格增减覆盖，NULL 表示沿用属性选项素材库默认价格增减。
     * 对应表字段：price_delta_override。
     */
    @TableField("price_delta_override")
    private BigDecimal priceDeltaOverride;

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
