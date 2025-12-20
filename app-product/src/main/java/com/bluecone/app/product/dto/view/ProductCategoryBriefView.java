package com.bluecone.app.product.dto.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品分类简要视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryBriefView {

    private Long categoryId;
    private String name;
    private Integer level;
    private Integer sortOrder;
}
