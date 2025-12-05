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
 * 商品 SKU 实体，对应表 {@code bc_product_sku}。
 * <p>
 * 该表记录商品在可售层面的具体单元（SKU），包含编码、定价、条形码、默认标记、规格组合等，
 * 负责支撑下单、库存、门店配置的最小可售粒度。
 */
@Data
@TableName("bc_product_sku")
public class ProductSkuEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * SKU ID，自增主键。
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
     * 所属商品ID（SPU）。
     * 对应表字段：product_id。
     */
    @TableField("product_id")
    private Long productId;

    /**
     * SKU 编码，租户内唯一，可用于对接收银、仓储等系统。
     * 对应表字段：sku_code。
     */
    @TableField("sku_code")
    private String skuCode;

    /**
     * SKU 名称，例如“中杯/热”。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 基础售价，订单计价的基础价格。
     * 对应表字段：base_price。
     */
    @TableField("base_price")
    private BigDecimal basePrice;

    /**
     * 划线价/原价，可选，用于展示优惠前价格。
     * 对应表字段：market_price。
     */
    @TableField("market_price")
    private BigDecimal marketPrice;

    /**
     * 成本价，可选，用于毛利分析。
     * 对应表字段：cost_price。
     */
    @TableField("cost_price")
    private BigDecimal costPrice;

    /**
     * 条形码或 PLU 码，便于线下收银或仓库管理。
     * 对应表字段：barcode。
     */
    @TableField("barcode")
    private String barcode;

    /**
     * 是否为默认 SKU：1是，0否。单规格商品通常为默认。
     * 对应表字段：is_default。
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 规格组合 JSON，如 [{"groupId":x,"optionId":y}]，描述该 SKU 由哪些规格项组成。
     * 对应表字段：spec_combination。
     */
    @TableField("spec_combination")
    private String specCombination;

    /**
     * 状态：1启用，0禁用，-1删除。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，用于 SKU 列表展示顺序。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * SKU 扩展字段，JSON 格式，用于存放额外业务属性。
     * 对应表字段：sku_meta。
     */
    @TableField("sku_meta")
    private String skuMeta;

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
