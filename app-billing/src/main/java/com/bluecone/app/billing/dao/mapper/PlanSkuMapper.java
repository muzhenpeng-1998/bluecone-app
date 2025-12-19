package com.bluecone.app.billing.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套餐 SKU Mapper
 */
@Mapper
public interface PlanSkuMapper extends BaseMapper<PlanSkuDO> {
}
