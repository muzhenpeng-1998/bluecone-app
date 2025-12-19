package com.bluecone.app.order.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.order.infra.persistence.po.OrderActionLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 订单动作幂等日志表 Mapper。
 */
@Mapper
@Repository("orderActionLogMapper")
public interface OrderActionLogMapper extends BaseMapper<OrderActionLogPO> {
}
