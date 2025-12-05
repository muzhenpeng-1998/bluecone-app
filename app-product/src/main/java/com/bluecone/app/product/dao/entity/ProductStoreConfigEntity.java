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
 * 门店维度商品配置实体，对应表 {@code bc_product_store_config}。
 * <p>
 * 在门店与渠道维度控制商品可见性、售价、可售订单类型与时间段等，支撑差异化定价、渠道上架和菜单排序，
 * 既可配置到 SPU 也可细化到 SKU 层级。
 */
@Data
@TableName("bc_product_store_config")
public class ProductStoreConfigEntity implements Serializable {

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
     * 门店ID。
     * 对应表字段：store_id。
     */
    @TableField("store_id")
    private Long storeId;

    /**
     * 商品ID（SPU）。
     * 对应表字段：product_id。
     */
    @TableField("product_id")
    private Long productId;

    /**
     * SKU ID，可为空表示在 SPU 层配置。
     * 对应表字段：sku_id。
     */
    @TableField("sku_id")
    private Long skuId;

    /**
     * 售卖渠道：ALL、DINE_IN、TAKEAWAY、DELIVERY、PICKUP 等。
     * 对应表字段：channel。
     */
    @TableField("channel")
    private String channel;

    /**
     * 是否在该门店该渠道可见：1是，0否。
     * 对应表字段：visible。
     */
    @TableField("visible")
    private Boolean visible;

    /**
     * 门店价/渠道价，NULL 表示使用 SKU 基础价。
     * 对应表字段：override_price。
     */
    @TableField("override_price")
    private BigDecimal overridePrice;

    /**
     * 可用订单类型数组，JSON 格式，例如 ["DINE_IN","DELIVERY"]。
     * 对应表字段：available_order_types。
     */
    @TableField("available_order_types")
    private String availableOrderTypes;

    /**
     * 当天可售时间段数组，JSON 格式，例如 [{"from":"07:00","to":"11:00"}]。
     * 对应表字段：available_time_ranges。
     */
    @TableField("available_time_ranges")
    private String availableTimeRanges;

    /**
     * 每日销量上限，NULL 表示不限制。
     * 对应表字段：daily_sold_out_limit。
     */
    @TableField("daily_sold_out_limit")
    private Integer dailySoldOutLimit;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 菜单中的排序值。
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
