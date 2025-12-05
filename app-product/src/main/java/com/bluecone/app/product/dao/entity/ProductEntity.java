package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品基础实体，对应表 {@code bc_product}。
 * <p>
 * 该表存储 SPU 级别的商品信息，描述“一个商品”的抽象定义：包含编码、名称、类型、主图及媒体资源、排序等基础属性，
 * 不区分具体规格，规格由 SKU 与规格组/规格项表承载。租户维度隔离，面向多租户 SaaS 场景。
 * <p>
 * 菜单展示、下单、库存、标签、门店配置等都会通过 productId 关联到本表，构成商品域的核心入口。
 */
@Data
@TableName("bc_product")
public class ProductEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID（SPU 主键，自增）。
     * 对应表字段：id。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户数据隔离。
     * 对应表字段：tenant_id。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 商品编码，租户内唯一，可用于外部对接、打印及内部管理。
     * 对应表字段：product_code。
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 商品名称，前台及后台展示的主名称。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 短标题或卖点，辅助展示商品亮点。
     * 对应表字段：subtitle。
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 商品类型：1餐饮/饮品，2服务，3场馆模板，4储值，5会员，6优惠券包等。
     * 对应表字段：product_type。
     */
    @TableField("product_type")
    private Integer productType;

    /**
     * 商品详情描述（富文本），用于商品详情页的图文展示。
     * 对应表字段：description。
     */
    @TableField("description")
    private String description;

    /**
     * 主图URL，通常用于列表或详情的首图展示。
     * 对应表字段：main_image。
     */
    @TableField("main_image")
    private String mainImage;

    /**
     * 媒体资源列表，JSON 格式，包含图集、视频等。
     * 对应表字段：media_gallery。
     */
    @TableField("media_gallery")
    private String mediaGallery;

    /**
     * 销售单位，如杯/份/小时/次等，用于前台展示和计价。
     * 对应表字段：unit。
     */
    @TableField("unit")
    private String unit;

    /**
     * 状态：0草稿，1启用（可上架），-1禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值，数值越大越靠前，用于商品列表排序。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 商品扩展字段，JSON 格式，用于美业/场馆/虚拟商品等特定信息。
     * 对应表字段：product_meta。
     */
    @TableField("product_meta")
    private String productMeta;

    /**
     * 创建时间，记录商品创建的时间戳。
     * 对应表字段：created_at。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间，记录最近一次修改的时间，自动更新。
     * 对应表字段：updated_at。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID，标识谁创建了该商品。
     * 对应表字段：created_by。
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 更新人ID，标识最近一次修改的用户。
     * 对应表字段：updated_by。
     */
    @TableField("updated_by")
    private Long updatedBy;
}
