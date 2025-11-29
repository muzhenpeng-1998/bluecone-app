// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/mapper/OutboxMessageMapper.java
package com.bluecone.app.infra.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper，用于 Outbox 表的 CRUD。
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessageEntity> {
}
