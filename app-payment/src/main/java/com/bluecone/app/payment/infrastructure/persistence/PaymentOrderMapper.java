package com.bluecone.app.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单持久化 Mapper。
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrderDO> {
}
