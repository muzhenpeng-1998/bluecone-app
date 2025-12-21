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
 * 商品绑定小料项实体，对应表 {@code bc_product_addon_rel}。
 * <p>
 * 定义商品启用哪些小料项及其小料项级覆盖规则（价格覆盖、最大数量覆盖、排序、启用状态），
 * 用于控制小料项在商品下的展示与计价规则。
 * <p>
 * 与 {@code bc_product_addon_group_rel} 的区别：
 * <ul>
 *   <li>group_rel：组级规则，控制整个小料组的必选性、选择范围、排序、定时展示</li>
 *   <li>本表（addon_rel）：小料项级覆盖，控制单个小料项的价格、最大数量、排序、启用状态</li>
 * </ul>
 */
@Data
@TableName("bc_product_addon_rel")
public class BcProductAddonRel implements Serializable {

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
     * 小料组ID。
     * 对应表字段：addon_group_id。
     */
    @TableField("addon_group_id")
    private Long addonGroupId;

    /**
     * 小料项ID。
     * 对应表字段：addon_item_id。
     */
    @TableField("addon_item_id")
    private Long addonItemId;

    /**
     * 价格覆盖，NULL 表示沿用小料项素材库默认价格。
     * 对应表字段：price_override。
     */
    @TableField("price_override")
    private BigDecimal priceOverride;

    /**
     * 最大数量覆盖，NULL 表示沿用小料项素材库默认最大数量。
     * 对应表字段：max_quantity_override。
     */
    @TableField("max_quantity_override")
    private BigDecimal maxQuantityOverride;

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
