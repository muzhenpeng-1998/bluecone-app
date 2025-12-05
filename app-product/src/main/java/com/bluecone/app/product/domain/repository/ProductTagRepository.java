package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.model.tag.ProductTag;
import java.util.List;

/**
 * 商品标签的领域仓储接口，维护标签定义及商品与标签的绑定关系。
 */
public interface ProductTagRepository {

    List<ProductTag> listByTenant(Long tenantId);

    List<ProductTag> listByProductId(Long tenantId, Long productId);

    void saveOrUpdate(ProductTag tag);

    void bindProductTag(Long tenantId, Long productId, Long tagId, Integer sortOrder);
}
