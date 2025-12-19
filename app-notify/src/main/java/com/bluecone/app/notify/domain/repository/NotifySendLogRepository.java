package com.bluecone.app.notify.domain.repository;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.domain.model.NotifySendLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送日志仓储接口
 */
public interface NotifySendLogRepository {
    
    Long save(NotifySendLog log);
    
    List<NotifySendLog> findByTaskId(Long taskId);
    
    List<NotifySendLog> findByUser(Long userId, NotificationChannel channel, LocalDateTime startTime, LocalDateTime endTime);
    
    int countByUserAndChannelSince(Long userId, NotificationChannel channel, LocalDateTime since);
    
    int countByUserAndTemplateCodeToday(Long userId, String templateCode);
}
