package com.bluecone.app.application.gateway.dto.product;

import com.bluecone.app.product.application.dto.UserStoreMenuView;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoreMenuResponse {

    private Long tenantId;

    private Long storeId;

    @Builder.Default
    private List<Category> categories = Collections.emptyList();

    public static UserStoreMenuResponse from(UserStoreMenuView view) {
        if (view == null) {
            return null;
        }
        List<Category> categories = view.getCategories() == null ? Collections.emptyList()
                : view.getCategories().stream()
                        .map(Category::from)
                        .collect(Collectors.toList());
        return UserStoreMenuResponse.builder()
                .tenantId(view.getTenantId())
                .storeId(view.getStoreId())
                .categories(categories)
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

        public static Category from(UserStoreMenuView.Category view) {
            if (view == null) {
                return null;
            }
            List<Item> items = view.getItems() == null ? Collections.emptyList()
                    : view.getItems().stream()
                            .map(Item::from)
                            .collect(Collectors.toList());
            return Category.builder()
                    .categoryId(view.getCategoryId())
                    .categoryName(view.getCategoryName())
                    .displayOrder(view.getDisplayOrder())
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

        public static Item from(UserStoreMenuView.Item view) {
            if (view == null) {
                return null;
            }
            return Item.builder()
                    .skuId(view.getSkuId())
                    .skuName(view.getSkuName())
                    .skuShortName(view.getSkuShortName())
                    .imageUrl(view.getImageUrl())
                    .salePrice(view.getSalePrice())
                    .available(view.getAvailable())
                    .build();
        }
    }
}
