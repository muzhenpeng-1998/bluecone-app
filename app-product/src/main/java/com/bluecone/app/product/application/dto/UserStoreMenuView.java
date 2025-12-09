package com.bluecone.app.product.application.dto;

import com.bluecone.app.product.domain.model.menu.UserStoreCategory;
import com.bluecone.app.product.domain.model.menu.UserStoreMenu;
import com.bluecone.app.product.domain.model.menu.UserStoreMenuItem;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供应用层/控制层消费的用户侧菜单视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoreMenuView {

    private Long tenantId;

    private Long storeId;

    @Builder.Default
    private List<Category> categories = Collections.emptyList();

    public static UserStoreMenuView from(UserStoreMenu menu) {
        if (menu == null) {
            return null;
        }
        List<Category> cats = menu.getCategories() == null ? Collections.emptyList()
                : menu.getCategories().stream()
                        .map(Category::from)
                        .collect(Collectors.toList());
        return UserStoreMenuView.builder()
                .tenantId(menu.getTenantId())
                .storeId(menu.getStoreId())
                .categories(cats)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private Long categoryId;
        private String categoryName;
        private Integer displayOrder;
        private List<Item> items;

        public static Category from(UserStoreCategory category) {
            if (category == null) {
                return null;
            }
            List<Item> items = category.getItems() == null ? Collections.emptyList()
                    : category.getItems().stream()
                            .map(Item::from)
                            .collect(Collectors.toList());
            return Category.builder()
                    .categoryId(category.getCategoryId())
                    .categoryName(category.getCategoryName())
                    .displayOrder(category.getDisplayOrder())
                    .items(items)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long skuId;
        private String skuName;
        private String skuShortName;
        private String imageUrl;
        private Long salePrice;
        private Boolean available;

        public static Item from(UserStoreMenuItem item) {
            if (item == null) {
                return null;
            }
            return Item.builder()
                    .skuId(item.getSkuId())
                    .skuName(item.getSkuName())
                    .skuShortName(item.getSkuShortName())
                    .imageUrl(item.getImageUrl())
                    .salePrice(item.getSalePrice())
                    .available(item.getAvailable())
                    .build();
        }
    }
}
