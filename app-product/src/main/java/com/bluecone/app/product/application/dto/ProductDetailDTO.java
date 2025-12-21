package com.bluecone.app.product.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 DTO
 * 
 * <p>用于查询商品完整聚合结构，包括商品基本信息、SKU、规格、属性、小料、分类绑定。
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    
    // ===== 商品基本信息 =====
    
    /**
     * 商品ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 商品Public ID
     */
    private String publicId;
    
    /**
     * 商品编码
     */
    private String productCode;
    
    /**
     * 商品名称
     */
    private String name;
    
    /**
     * 副标题
     */
    private String subtitle;
    
    /**
     * 商品类型
     */
    private Integer productType;
    
    /**
     * 商品描述
     */
    private String description;
    
    /**
     * 主图URL
     */
    private String mainImage;
    
    /**
     * 媒体资源列表
     */
    private List<String> mediaGallery;
    
    /**
     * 销售单位
     */
    private String unit;
    
    /**
     * 状态（0=草稿，1=启用，-1=禁用）
     */
    private Integer status;
    
    /**
     * 排序值
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // ===== SKU 列表 =====
    
    /**
     * SKU列表
     */
    private List<SkuDTO> skus;
    
    // ===== 规格组列表 =====
    
    /**
     * 规格组列表
     */
    private List<SpecGroupDTO> specGroups;
    
    // ===== 属性组绑定列表 =====
    
    /**
     * 属性组绑定列表
     */
    private List<AttrGroupBindingDTO> attrGroups;
    
    // ===== 小料组绑定列表 =====
    
    /**
     * 小料组绑定列表
     */
    private List<AddonGroupBindingDTO> addonGroups;
    
    // ===== 分类绑定列表 =====
    
    /**
     * 分类绑定列表
     */
    private List<CategoryBindingDTO> categories;
    
    // ===== 内部类：SKU DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuDTO {
        
        /**
         * SKU ID
         */
        private Long id;
        
        /**
         * SKU Public ID
         */
        private String publicId;
        
        /**
         * SKU编码
         */
        private String skuCode;
        
        /**
         * SKU名称
         */
        private String name;
        
        /**
         * 基础价格
         */
        private BigDecimal basePrice;
        
        /**
         * 市场价格
         */
        private BigDecimal marketPrice;
        
        /**
         * 成本价格
         */
        private BigDecimal costPrice;
        
        /**
         * 条形码
         */
        private String barcode;
        
        /**
         * 是否默认SKU
         */
        private Boolean defaultSku;
        
        /**
         * 状态
         */
        private Integer status;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
    }
    
    // ===== 内部类：规格组 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecGroupDTO {
        
        /**
         * 规格组ID
         */
        private Long id;
        
        /**
         * 规格组名称
         */
        private String name;
        
        /**
         * 选择类型（1=单选，2=多选）
         */
        private Integer selectType;
        
        /**
         * 是否必选
         */
        private Boolean required;
        
        /**
         * 最大选择数量
         */
        private Integer maxSelect;
        
        /**
         * 状态
         */
        private Integer status;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
        
        /**
         * 规格选项列表
         */
        private List<SpecOptionDTO> options;
    }
    
    // ===== 内部类：规格选项 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecOptionDTO {
        
        /**
         * 规格选项ID
         */
        private Long id;
        
        /**
         * 规格选项名称
         */
        private String name;
        
        /**
         * 价格增量
         */
        private BigDecimal priceDelta;
        
        /**
         * 是否默认选中
         */
        private Boolean isDefault;
        
        /**
         * 状态
         */
        private Integer status;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
    }
    
    // ===== 内部类：属性组绑定 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrGroupBindingDTO {
        
        /**
         * 属性组ID
         */
        private Long groupId;
        
        /**
         * 属性组名称
         */
        private String groupName;
        
        /**
         * 是否必选
         */
        private Boolean required;
        
        /**
         * 最小选择数量
         */
        private Integer minSelect;
        
        /**
         * 最大选择数量
         */
        private Integer maxSelect;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 属性选项列表（含覆盖）
         */
        private List<AttrOptionDTO> options;
    }
    
    // ===== 内部类：属性选项 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrOptionDTO {
        
        /**
         * 属性选项ID
         */
        private Long id;
        
        /**
         * 属性选项名称
         */
        private String name;
        
        /**
         * 价格增量（已应用覆盖）
         */
        private BigDecimal priceDelta;
        
        /**
         * 排序值（已应用覆盖）
         */
        private Integer sortOrder;
        
        /**
         * 是否启用（已应用覆盖）
         */
        private Boolean enabled;
    }
    
    // ===== 内部类：小料组绑定 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonGroupBindingDTO {
        
        /**
         * 小料组ID
         */
        private Long groupId;
        
        /**
         * 小料组名称
         */
        private String groupName;
        
        /**
         * 是否必选
         */
        private Boolean required;
        
        /**
         * 最小选择数量
         */
        private Integer minSelect;
        
        /**
         * 最大选择数量
         */
        private Integer maxSelect;
        
        /**
         * 总可选上限
         */
        private BigDecimal maxTotal;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 小料项列表（含覆盖）
         */
        private List<AddonItemDTO> items;
    }
    
    // ===== 内部类：小料项 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonItemDTO {
        
        /**
         * 小料项ID
         */
        private Long id;
        
        /**
         * 小料项名称
         */
        private String name;
        
        /**
         * 价格（已应用覆盖）
         */
        private BigDecimal price;
        
        /**
         * 最大数量（已应用覆盖）
         */
        private BigDecimal maxQuantity;
        
        /**
         * 排序值（已应用覆盖）
         */
        private Integer sortOrder;
        
        /**
         * 是否启用（已应用覆盖）
         */
        private Boolean enabled;
    }
    
    // ===== 内部类：分类绑定 DTO =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBindingDTO {
        
        /**
         * 分类ID
         */
        private Long categoryId;
        
        /**
         * 分类名称
         */
        private String categoryName;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
    }
}

