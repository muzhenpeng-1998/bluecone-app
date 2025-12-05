package com.bluecone.app.product.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.product.dao.entity.ProductAttrGroupEntity;

/**
 * 通用属性组表 Mapper，对应表 {@code bc_product_attr_group}，维护租户层可复用的属性组配置。
 */
public interface ProductAttrGroupMapper extends BaseMapper<ProductAttrGroupEntity> {
}
