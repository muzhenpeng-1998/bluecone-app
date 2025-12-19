package com.bluecone.app.infra.event.consume;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 事件消费日志 Mapper
 */
@Mapper
public interface EventConsumeLogMapper extends BaseMapper<EventConsumeLogPO> {
    
    /**
     * 检查事件是否已被消费（幂等性检查）
     * 
     * @param consumerName 消费者名称
     * @param eventId 事件ID
     * @return 消费日志（如果存在）
     */
    @Select("SELECT * FROM bc_event_consume_log " +
            "WHERE consumer_name = #{consumerName} " +
            "AND event_id = #{eventId} " +
            "LIMIT 1")
    EventConsumeLogPO selectByConsumerAndEvent(@Param("consumerName") String consumerName, 
                                                @Param("eventId") String eventId);
    
    /**
     * 检查幂等键是否已被消费
     * 
     * @param idempotencyKey 幂等键
     * @return 消费日志（如果存在）
     */
    @Select("SELECT * FROM bc_event_consume_log " +
            "WHERE idempotency_key = #{idempotencyKey} " +
            "AND status = 'SUCCESS' " +
            "LIMIT 1")
    EventConsumeLogPO selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
