package com.bluecone.app.infra.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper：集成投递任务。
 */
@Mapper
public interface IntegrationDeliveryMapper extends BaseMapper<IntegrationDeliveryEntity> {
}
