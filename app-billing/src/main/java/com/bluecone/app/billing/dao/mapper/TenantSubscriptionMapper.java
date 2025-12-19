package com.bluecone.app.billing.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户订阅 Mapper
 */
@Mapper
public interface TenantSubscriptionMapper extends BaseMapper<TenantSubscriptionDO> {
}
