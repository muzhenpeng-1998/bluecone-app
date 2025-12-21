package com.bluecone.app.product.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 门店商品排序请求
 * 
 * <p>用于批量调整商品在门店菜单中的排序。
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductReorderRequest {
    
    /**
     * 租户ID（必填）
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    /**
     * 操作人ID（必填）
     */
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
    
    /**
     * 售卖渠道（可选，默认 ALL）
     */
    private String channel;
    
    /**
     * 商品排序列表（必填）
     */
    @NotEmpty(message = "商品排序列表不能为空")
    private List<ProductSortItem> products;
    
    /**
     * 商品排序项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSortItem {
        
        /**
         * 商品ID（必填）
         */
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        
        /**
         * 排序值（必填）
         * 降序排列：值越大越靠前
         */
        @NotNull(message = "排序值不能为空")
        private Integer sortOrder;
    }
}

