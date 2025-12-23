package com.bluecone.app.payment.simple.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.payment.simple.infrastructure.persistence.PaymentOrderDO;

/**
 * 简化支付单的 MyBatis-Plus Mapper。
 */
@Mapper
public interface SimplePaymentOrderMapper extends BaseMapper<PaymentOrderDO> {
}
