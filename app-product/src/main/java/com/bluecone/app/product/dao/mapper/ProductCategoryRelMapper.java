package com.bluecone.app.product.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.product.dao.entity.ProductCategoryRelEntity;

/**
 * 商品-分类关联表 Mapper，对应表 {@code bc_product_category_rel}，用于维护商品与分类的多对多关系。
 */
public interface ProductCategoryRelMapper extends BaseMapper<ProductCategoryRelEntity> {
}
