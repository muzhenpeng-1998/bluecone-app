package com.bluecone.app.notify.domain.repository;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.domain.model.NotifyTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板仓储接口
 */
public interface NotifyTemplateRepository {
    
    Long save(NotifyTemplate template);
    
    boolean update(NotifyTemplate template);
    
    boolean delete(Long id);
    
    Optional<NotifyTemplate> findById(Long id);
    
    Optional<NotifyTemplate> findByCodeAndChannel(String templateCode, NotificationChannel channel, Long tenantId);
    
    List<NotifyTemplate> findByBizType(String bizType, Long tenantId);
    
    List<NotifyTemplate> findAll(Long tenantId);
}
