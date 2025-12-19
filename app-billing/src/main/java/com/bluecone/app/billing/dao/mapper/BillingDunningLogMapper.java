package com.bluecone.app.billing.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.billing.dao.entity.BillingDunningLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Dunning 发送日志 Mapper
 */
@Mapper
public interface BillingDunningLogMapper extends BaseMapper<BillingDunningLogDO> {
}
