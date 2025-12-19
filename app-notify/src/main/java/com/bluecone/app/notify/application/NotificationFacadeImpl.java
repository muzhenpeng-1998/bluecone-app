package com.bluecone.app.notify.application;

import com.bluecone.app.notify.api.dto.EnqueueNotificationRequest;
import com.bluecone.app.notify.api.dto.EnqueueNotificationResponse;
import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.facade.NotificationFacade;
import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.repository.NotifyTaskRepository;
import com.bluecone.app.notify.domain.service.NotifyTaskCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 通知服务门面实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {
    
    private final NotifyTaskCreator taskCreator;
    private final NotifyTaskRepository taskRepository;
    
    @Override
    public EnqueueNotificationResponse enqueue(EnqueueNotificationRequest request) {
        log.info("Enqueue notification: bizType={}, bizId={}, userId={}, channels={}", 
                request.getBizType(), request.getBizId(), request.getUserId(), request.getChannels());
        
        List<Long> taskIds = new ArrayList<>();
        List<EnqueueNotificationResponse.FailedChannel> failedChannels = new ArrayList<>();
        
        // 为每个渠道创建任务
        for (NotificationChannel channel : request.getChannels()) {
            try {
                Long taskId = taskCreator.createTaskForChannel(
                        request.getBizType(),
                        request.getBizId(),
                        request.getTenantId(),
                        request.getUserId(),
                        channel,
                        request.getTemplateCode(),
                        request.getVariables()
                );
                
                if (taskId != null) {
                    taskIds.add(taskId);
                } else {
                    failedChannels.add(EnqueueNotificationResponse.FailedChannel.builder()
                            .channel(channel.name())
                            .reason("Template not found or disabled")
                            .build());
                }
            } catch (Exception e) {
                log.error("Failed to create task for channel={}", channel, e);
                failedChannels.add(EnqueueNotificationResponse.FailedChannel.builder()
                        .channel(channel.name())
                        .reason(e.getMessage())
                        .build());
            }
        }
        
        return EnqueueNotificationResponse.builder()
                .taskIds(taskIds)
                .allCreated(failedChannels.isEmpty())
                .failedChannels(failedChannels)
                .build();
    }
    
    @Override
    public boolean cancelTask(Long taskId) {
        Optional<NotifyTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return false;
        }
        
        NotifyTask task = taskOpt.get();
        task.markAsCancelled();
        return taskRepository.update(task);
    }
    
    @Override
    public String getTaskStatus(Long taskId) {
        return taskRepository.findById(taskId)
                .map(task -> task.getStatus().name())
                .orElse(null);
    }
}
