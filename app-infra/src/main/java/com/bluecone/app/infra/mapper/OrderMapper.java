package com.bluecone.app.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层（MyBatis-Plus）
 * 继承 BaseMapper 即可获得 CRUD 能力
 * 多租户拦截器会自动注入 tenant_id 条件
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
