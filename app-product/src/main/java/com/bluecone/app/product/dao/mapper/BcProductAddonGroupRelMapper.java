package com.bluecone.app.product.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.product.dao.entity.BcProductAddonGroupRel;

/**
 * 商品-小料组绑定表 Mapper，对应表 {@code bc_product_addon_group_rel}。
 * <p>
 * 用于管理商品启用的小料组及组级规则（必选性、选择范围、总可选上限、排序、定时展示）。
 */
public interface BcProductAddonGroupRelMapper extends BaseMapper<BcProductAddonGroupRel> {
}

