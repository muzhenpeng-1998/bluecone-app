package com.bluecone.app.infra.event.consume;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件消费去重记录 Mapper。
 */
@Mapper
public interface EventConsumeRecordMapper extends BaseMapper<EventConsumeRecordDO> {
}

