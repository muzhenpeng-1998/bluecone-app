package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper for bc_outbox_message.
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventDO> {
}


