package com.bluecone.app.infra.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：集成订阅。
 */
@Mapper
public interface IntegrationSubscriptionMapper extends BaseMapper<IntegrationSubscriptionEntity> {
}
