package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 事件 Mapper
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventPO> {
    
    /**
     * 查询待投递的事件（状态为 NEW 或 FAILED，且到达重试时间）
     * 
     * @param limit 限制数量
     * @param now 当前时间
     * @return 待投递事件列表
     */
    @Select("SELECT * FROM bc_outbox_event " +
            "WHERE status IN ('NEW', 'FAILED') " +
            "AND next_retry_at <= #{now} " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<OutboxEventPO> selectPendingEvents(@Param("limit") int limit, @Param("now") LocalDateTime now);
    
    /**
     * 查询指定聚合根的所有事件
     * 
     * @param aggregateType 聚合根类型
     * @param aggregateId 聚合根ID
     * @return 事件列表
     */
    @Select("SELECT * FROM bc_outbox_event " +
            "WHERE aggregate_type = #{aggregateType} " +
            "AND aggregate_id = #{aggregateId} " +
            "ORDER BY created_at ASC")
    List<OutboxEventPO> selectByAggregate(@Param("aggregateType") String aggregateType, 
                                          @Param("aggregateId") String aggregateId);
}
