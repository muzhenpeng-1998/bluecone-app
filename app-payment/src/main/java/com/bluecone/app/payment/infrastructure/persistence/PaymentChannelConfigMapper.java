package com.bluecone.app.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付渠道配置持久化 Mapper。
 */
@Mapper
public interface PaymentChannelConfigMapper extends BaseMapper<PaymentChannelConfigDO> {
}
