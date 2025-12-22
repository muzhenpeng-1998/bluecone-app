package com.bluecone.app.product.application.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类排序项
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReorderItem {
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 新的排序值
     */
    private Integer sortOrder;
}

