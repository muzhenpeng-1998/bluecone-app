package com.bluecone.app.notify.domain.repository;

import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import com.bluecone.app.notify.domain.model.NotifyTask;

import java.util.List;
import java.util.Optional;

/**
 * 通知任务仓储接口
 */
public interface NotifyTaskRepository {
    
    Long save(NotifyTask task);
    
    boolean update(NotifyTask task);
    
    Optional<NotifyTask> findById(Long id);
    
    Optional<NotifyTask> findByIdempotencyKey(String idempotencyKey);
    
    List<NotifyTask> findPendingTasks(int limit);
    
    List<NotifyTask> findTasksForRetry(int limit);
    
    List<NotifyTask> findByUserAndBizType(Long userId, String bizType, int limit);
    
    int countByStatus(NotificationTaskStatus status);
}
