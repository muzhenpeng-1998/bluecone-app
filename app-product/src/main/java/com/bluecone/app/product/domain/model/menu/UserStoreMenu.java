package com.bluecone.app.product.domain.model.menu;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户侧门店菜单聚合，按分类组织商品项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoreMenu {

    private Long tenantId;

    private Long storeId;

    @Builder.Default
    private List<UserStoreCategory> categories = Collections.emptyList();

    /**
     * 空菜单工厂方法。
     */
    public static UserStoreMenu empty(Long tenantId, Long storeId) {
        return UserStoreMenu.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .categories(Collections.emptyList())
                .build();
    }
}
