package com.bluecone.app.product.domain.service;

import com.bluecone.app.product.domain.model.Product;

/**
 * 商品领域服务，占位用于编排商品聚合的跨实体规则（如上下架校验、复制合并等）。
 */
public interface ProductDomainService {

    /**
     * 校验商品是否满足发布/上架条件（占位，后续补充具体规则）。
     */
    void validateForPublish(Product product);
}
