package com.bluecone.app.product.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建商品聚合命令
 * 
 * <p>包含商品基本信息、SKU、规格、属性、小料、分类绑定的完整聚合结构。
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductAggregateCommand {
    
    /**
     * 租户ID（必填）
     */
    private Long tenantId;
    
    /**
     * 操作人ID（必填）
     */
    private Long operatorId;
    
    // ===== 商品基本信息 =====
    
    /**
     * 商品编码（可选）
     */
    private String productCode;
    
    /**
     * 商品名称（必填）
     */
    private String name;
    
    /**
     * 副标题（可选）
     */
    private String subtitle;
    
    /**
     * 商品类型（必填）
     */
    private Integer productType;
    
    /**
     * 商品描述（可选）
     */
    private String description;
    
    /**
     * 主图URL（可选）
     */
    private String mainImage;
    
    /**
     * 媒体资源列表（可选）
     */
    private List<String> mediaGallery;
    
    /**
     * 销售单位（可选）
     */
    private String unit;
    
    /**
     * 排序值（可选，默认0）
     */
    private Integer sortOrder;
    
    // ===== SKU 列表 =====
    
    /**
     * SKU列表（必填，至少1个）
     */
    private List<SkuRequest> skus;
    
    // ===== 规格组列表 =====
    
    /**
     * 规格组列表（可选）
     */
    private List<SpecGroupRequest> specGroups;
    
    // ===== 属性组绑定列表 =====
    
    /**
     * 属性组绑定列表（可选）
     */
    private List<AttrGroupBinding> attrGroups;
    
    // ===== 小料组绑定列表 =====
    
    /**
     * 小料组绑定列表（可选）
     */
    private List<AddonGroupBinding> addonGroups;
    
    // ===== 分类绑定列表 =====
    
    /**
     * 分类ID列表（可选）
     */
    private List<Long> categoryIds;
    
    // ===== Prompt 06: 创建后立即上架 =====
    
    /**
     * 门店ID（可选）
     * <p>如果传了，创建完成后自动插入 store_config（visible=true，status=1）
     */
    private Long storeId;
    
    /**
     * 售卖渠道（可选，默认 ALL）
     * <p>与 storeId 配合使用
     */
    private String channel;
    
    /**
     * 是否立即发布（可选，默认 false）
     * <p>true: product.status=1, sku.status=1（立即可见）
     * <p>false: product.status=0, sku.status=0（草稿状态）
     */
    private Boolean publishNow;
    
    // ===== 内部类：SKU 请求 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuRequest {
        
        /**
         * SKU编码（可选）
         */
        private String skuCode;
        
        /**
         * SKU名称（必填）
         */
        private String name;
        
        /**
         * 基础价格（必填）
         */
        private BigDecimal basePrice;
        
        /**
         * 市场价格（可选）
         */
        private BigDecimal marketPrice;
        
        /**
         * 成本价格（可选）
         */
        private BigDecimal costPrice;
        
        /**
         * 条形码（可选）
         */
        private String barcode;
        
        /**
         * 是否默认SKU（必填）
         */
        private boolean defaultSku;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 规格组合（可选）
         */
        private List<SpecCombination> specCombination;
    }
    
    // ===== 内部类：规格组合 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecCombination {
        
        /**
         * 规格组名称
         */
        private String groupName;
        
        /**
         * 规格选项名称
         */
        private String optionName;
        
        /**
         * 规格选项ID（可选）
         */
        private Long optionId;
    }
    
    // ===== 内部类：规格组请求 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecGroupRequest {
        
        /**
         * 规格组名称（必填）
         */
        private String name;
        
        /**
         * 选择类型（必填，1=单选，2=多选）
         */
        private Integer selectType;
        
        /**
         * 是否必选（必填）
         */
        private Boolean required;
        
        /**
         * 最大选择数量（可选）
         */
        private Integer maxSelect;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 规格选项列表（必填，至少1个）
         */
        private List<SpecOptionRequest> options;
    }
    
    // ===== 内部类：规格选项请求 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecOptionRequest {
        
        /**
         * 规格选项名称（必填）
         */
        private String name;
        
        /**
         * 价格增量（可选，默认0）
         */
        private BigDecimal priceDelta;
        
        /**
         * 是否默认选中（可选，默认false）
         */
        private Boolean isDefault;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
    }
    
    // ===== 内部类：属性组绑定 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrGroupBinding {
        
        /**
         * 属性组ID（必填）
         */
        private Long groupId;
        
        /**
         * 是否必选（必填）
         */
        private Boolean required;
        
        /**
         * 最小选择数量（必填）
         */
        private Integer minSelect;
        
        /**
         * 最大选择数量（可选）
         */
        private Integer maxSelect;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 是否启用（必填）
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间（可选）
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间（可选）
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 属性选项覆盖列表（可选）
         */
        private List<AttrOptionOverride> optionOverrides;
    }
    
    // ===== 内部类：属性选项覆盖 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrOptionOverride {
        
        /**
         * 属性选项ID（必填）
         */
        private Long optionId;
        
        /**
         * 是否启用（必填）
         */
        private Boolean enabled;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 价格增量覆盖（可选）
         */
        private BigDecimal priceDeltaOverride;
    }
    
    // ===== 内部类：小料组绑定 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonGroupBinding {
        
        /**
         * 小料组ID（必填）
         */
        private Long groupId;
        
        /**
         * 是否必选（必填）
         */
        private Boolean required;
        
        /**
         * 最小选择数量（必填）
         */
        private Integer minSelect;
        
        /**
         * 最大选择数量（可选）
         */
        private Integer maxSelect;
        
        /**
         * 总可选上限（可选）
         */
        private BigDecimal maxTotal;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 是否启用（必填）
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间（可选）
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间（可选）
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 小料项覆盖列表（可选）
         */
        private List<AddonItemOverride> itemOverrides;
    }
    
    // ===== 内部类：小料项覆盖 =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonItemOverride {
        
        /**
         * 小料项ID（必填）
         */
        private Long itemId;
        
        /**
         * 是否启用（必填）
         */
        private Boolean enabled;
        
        /**
         * 排序值（可选，默认0）
         */
        private Integer sortOrder;
        
        /**
         * 价格覆盖（可选）
         */
        private BigDecimal priceOverride;
        
        /**
         * 最大数量覆盖（可选）
         */
        private BigDecimal maxQuantityOverride;
    }
}

