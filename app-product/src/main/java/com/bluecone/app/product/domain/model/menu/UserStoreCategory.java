package com.bluecone.app.product.domain.model.menu;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户侧分类。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStoreCategory {

    private Long categoryId;

    private String categoryName;

    private Integer displayOrder;

    @Builder.Default
    private List<UserStoreMenuItem> items = Collections.emptyList();
}
