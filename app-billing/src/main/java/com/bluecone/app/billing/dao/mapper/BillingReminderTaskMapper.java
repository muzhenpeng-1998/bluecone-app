package com.bluecone.app.billing.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.billing.dao.entity.BillingReminderTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅到期提醒任务 Mapper
 */
@Mapper
public interface BillingReminderTaskMapper extends BaseMapper<BillingReminderTaskDO> {
}
