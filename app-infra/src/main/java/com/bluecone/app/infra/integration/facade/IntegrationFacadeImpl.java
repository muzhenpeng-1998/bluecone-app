package com.bluecone.app.infra.integration.facade;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.integration.IntegrationFacade;
import com.bluecone.app.infra.integration.service.IntegrationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * IntegrationFacade 实现：委托 IntegrationDispatchService。
 */
@Service
public class IntegrationFacadeImpl implements IntegrationFacade {

    private static final Logger log = LoggerFactory.getLogger(IntegrationFacadeImpl.class);

    private final IntegrationDispatchService dispatchService;

    public IntegrationFacadeImpl(final IntegrationDispatchService dispatchService) {
        this.dispatchService = Objects.requireNonNull(dispatchService, "dispatchService must not be null");
    }

    @Override
    public void publishIntegration(final DomainEvent event) {
        log.info("[Integration] receive eventId={} type={} tenant={}", event.getEventId(), event.getEventType(), event.getTenantId());
        dispatchService.onDomainEvent(event);
    }
}
