package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知任务 Mapper
 */
@Mapper
public interface NotifyTaskMapper extends BaseMapper<NotifyTaskDO> {
    
    /**
     * 查询待发送的任务（PENDING状态）
     */
    @Select("SELECT * FROM bc_notify_task WHERE status = 'PENDING' ORDER BY priority DESC, created_at ASC LIMIT #{limit}")
    List<NotifyTaskDO> selectPendingTasks(@Param("limit") int limit);
    
    /**
     * 查询待重试的任务（FAILED状态且可重试）
     */
    @Select("SELECT * FROM bc_notify_task WHERE status = 'FAILED' AND retry_count < max_retry_count " +
            "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) ORDER BY priority DESC, next_retry_at ASC LIMIT #{limit}")
    List<NotifyTaskDO> selectTasksForRetry(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
