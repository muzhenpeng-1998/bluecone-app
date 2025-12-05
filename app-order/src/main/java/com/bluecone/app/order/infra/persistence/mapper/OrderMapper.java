package com.bluecone.app.order.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("orderModuleOrderMapper")
public interface OrderMapper extends BaseMapper<OrderPO> {
}
