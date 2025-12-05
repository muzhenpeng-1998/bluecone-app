package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单分类视图，包含分类下的商品列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuCategoryView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String name;
    private Integer sortOrder;
    private List<StoreMenuProductView> products;
}
