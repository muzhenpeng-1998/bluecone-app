package com.bluecone.app.notify.domain.service;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import com.bluecone.app.notify.domain.model.NotifyTask;
import com.bluecone.app.notify.domain.model.NotifyTemplate;
import com.bluecone.app.notify.domain.policy.NotificationPolicy;
import com.bluecone.app.notify.domain.policy.NotificationPolicyRegistry;
import com.bluecone.app.notify.domain.repository.NotifyTaskRepository;
import com.bluecone.app.notify.domain.repository.NotifyTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知任务创建器
 * 消费 outbox 事件，根据策略生成 notify_task
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyTaskCreator {
    
    private final NotificationPolicyRegistry policyRegistry;
    private final NotifyTemplateRepository templateRepository;
    private final NotifyTaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 根据业务事件创建通知任务
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param eventPayload 事件载荷
     * @return 创建的任务ID列表
     */
    @Transactional
    public List<Long> createTasks(String bizType, String bizId, Long tenantId, Long userId, Object eventPayload) {
        log.info("Creating notify tasks for bizType={}, bizId={}, userId={}", bizType, bizId, userId);
        
        List<Long> taskIds = new ArrayList<>();
        
        // 1. 查找策略
        Optional<NotificationPolicy> policyOpt = policyRegistry.getPolicy(bizType);
        if (policyOpt.isEmpty()) {
            log.warn("No notification policy found for bizType={}", bizType);
            return taskIds;
        }
        
        NotificationPolicy policy = policyOpt.get();
        
        // 2. 提取变量
        Map<String, Object> variables = policy.extractVariables(eventPayload);
        
        // 3. 为每个渠道创建任务
        for (NotificationChannel channel : policy.getSupportedChannels()) {
            try {
                Long taskId = createTaskForChannel(bizType, bizId, tenantId, userId, channel, 
                                                   policy.getTemplateCode(), variables);
                if (taskId != null) {
                    taskIds.add(taskId);
                }
            } catch (Exception e) {
                log.error("Failed to create task for channel={}", channel, e);
            }
        }
        
        log.info("Created {} notify tasks for bizType={}, bizId={}", taskIds.size(), bizType, bizId);
        return taskIds;
    }
    
    /**
     * 为指定渠道创建任务（暴露给 Facade 使用）
     */
    @Transactional
    public Long createTaskForChannel(String bizType, String bizId, Long tenantId, Long userId,
                                     NotificationChannel channel, String templateCode,
                                     Map<String, Object> variables) {
        
        // 1. 生成幂等键
        String idempotencyKey = buildIdempotencyKey(tenantId, bizType, bizId, channel);
        
        // 2. 检查是否已存在
        Optional<NotifyTask> existingTask = taskRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTask.isPresent()) {
            log.info("Task already exists with idempotencyKey={}", idempotencyKey);
            return existingTask.get().getId();
        }
        
        // 3. 查找模板
        Optional<NotifyTemplate> templateOpt = templateRepository.findByCodeAndChannel(templateCode, channel, tenantId);
        if (templateOpt.isEmpty() || !templateOpt.get().isEnabled()) {
            log.warn("Template not found or disabled: templateCode={}, channel={}", templateCode, channel);
            return null;
        }
        
        NotifyTemplate template = templateOpt.get();
        
        // 4. 渲染内容
        String title = template.renderTitle(variables);
        String content = template.renderContent(variables);
        
        // 5. 创建任务
        NotifyTask task = NotifyTask.builder()
                .tenantId(tenantId)
                .userId(userId)
                .bizType(bizType)
                .bizId(bizId)
                .templateCode(templateCode)
                .channel(channel)
                .priority(template.getPriority() != null ? template.getPriority() : 50)
                .variablesJson(toJson(variables))
                .title(title)
                .content(content)
                .idempotencyKey(idempotencyKey)
                .status(NotificationTaskStatus.PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .build();
        
        Long taskId = taskRepository.save(task);
        log.info("Created notify task: id={}, channel={}, templateCode={}", taskId, channel, templateCode);
        
        return taskId;
    }
    
    /**
     * 构建幂等键：tenantId:bizType:bizId:channel
     */
    private String buildIdempotencyKey(Long tenantId, String bizType, String bizId, NotificationChannel channel) {
        return String.format("%s:%s:%s:%s", 
                            tenantId != null ? tenantId : "null", 
                            bizType, 
                            bizId, 
                            channel.name());
    }
    
    /**
     * 对象转JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }
}
