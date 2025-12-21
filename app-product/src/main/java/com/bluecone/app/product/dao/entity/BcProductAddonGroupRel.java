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
 * 商品绑定小料组实体，对应表 {@code bc_product_addon_group_rel}。
 * <p>
 * 定义商品启用哪些小料组及组级规则（是否必选、最小/最大选择数量、总可选上限、排序、定时展示），
 * 用于控制小料组在商品下的展示与选择规则。
 * <p>
 * 与 {@code bc_product_addon_rel} 的区别：
 * <ul>
 *   <li>本表（group_rel）：组级规则，控制整个小料组的必选性、选择范围、排序、定时展示</li>
 *   <li>addon_rel：小料项级覆盖，控制单个小料项的禁用、排序、价格/数量覆盖</li>
 * </ul>
 */
@Data
@TableName("bc_product_addon_group_rel")
public class BcProductAddonGroupRel implements Serializable {

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
     * 该小料组在本商品下的总可选上限（数量），支持小数以兼容重量型加料。
     * 对应表字段：max_total_quantity。
     */
    @TableField("max_total_quantity")
    private BigDecimal maxTotalQuantity;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，用于控制小料组在商品下的展示顺序。
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

