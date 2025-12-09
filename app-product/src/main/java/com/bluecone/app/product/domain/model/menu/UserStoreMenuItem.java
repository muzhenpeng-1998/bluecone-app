package com.bluecone.app.product.domain.model.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户侧菜品项视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoreMenuItem {

    private Long skuId;

    private String skuName;

    private String skuShortName;

    private String imageUrl;

    private Long salePrice;

    private Boolean available;

    private String soldOutReason;

    private Integer displayOrder;
}
