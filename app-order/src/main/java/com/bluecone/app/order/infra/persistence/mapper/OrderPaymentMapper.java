package com.bluecone.app.order.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.order.infra.persistence.po.OrderPaymentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPaymentPO> {
}
