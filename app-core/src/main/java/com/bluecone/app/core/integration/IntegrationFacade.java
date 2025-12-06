package com.bluecone.app.core.integration;

import com.bluecone.app.core.event.DomainEvent;

/**
 * 业务侧极简入口：将领域事件交给 Integration Hub。
 */
public interface IntegrationFacade {

    /**
     * 发布需要对外集成的事件。
     *
     * @param event 领域事件
     */
    void publishIntegration(DomainEvent event);
}
