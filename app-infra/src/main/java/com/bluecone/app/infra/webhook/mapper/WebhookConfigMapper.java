package com.bluecone.app.infra.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.webhook.entity.WebhookConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for bc_webhook_config.
 */
@Mapper
public interface WebhookConfigMapper extends BaseMapper<WebhookConfigDO> {
}
