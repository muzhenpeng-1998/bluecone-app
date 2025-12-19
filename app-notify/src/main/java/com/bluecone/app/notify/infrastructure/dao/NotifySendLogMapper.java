package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知发送日志 Mapper
 */
@Mapper
public interface NotifySendLogMapper extends BaseMapper<NotifySendLogDO> {
    
    /**
     * 统计用户在指定渠道从某时间点至今的发送次数
     */
    @Select("SELECT COUNT(*) FROM bc_notify_send_log WHERE user_id = #{userId} AND channel = #{channel} " +
            "AND sent_at >= #{since} AND send_status = 'SUCCESS'")
    int countByUserAndChannelSince(@Param("userId") Long userId, 
                                    @Param("channel") String channel, 
                                    @Param("since") String since);
}
