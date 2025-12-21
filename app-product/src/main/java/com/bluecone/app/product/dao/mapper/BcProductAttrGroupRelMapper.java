package com.bluecone.app.product.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.product.dao.entity.BcProductAttrGroupRel;

/**
 * 商品-属性组绑定表 Mapper，对应表 {@code bc_product_attr_group_rel}。
 * <p>
 * 用于管理商品启用的属性组及组级规则（必选性、选择范围、排序、定时展示）。
 */
public interface BcProductAttrGroupRelMapper extends BaseMapper<BcProductAttrGroupRel> {
}

