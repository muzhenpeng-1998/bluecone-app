package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.model.ProductCategory;
import java.util.Collection;
import java.util.List;

/**
 * 商品分类/菜单分组的领域仓储接口，维护分类树及商品与分类的多对多绑定。
 */
public interface ProductCategoryRepository {

    List<ProductCategory> listByTenant(Long tenantId);

    List<ProductCategory> listByTenantAndStatus(Long tenantId, ProductStatus status);

    List<ProductCategory> listByProductId(Long tenantId, Long productId);

    void saveOrUpdate(ProductCategory category);

    void bindProductToCategory(Long tenantId, Long productId, Long categoryId, Integer sortOrder);

    void bindProductToCategories(Long tenantId, Long productId, Collection<Long> categoryIds);
}
