package com.bluecone.app.application.outbox;

import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default handler that logs every event until a richer dispatch is built.
 * 
 * Note: 此为旧版本实现，仅在 dev/test 环境使用。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class LoggingOutboxEventHandler implements OutboxEventHandler {

    @Override
    public boolean supports(OutboxEventDO event) {
        return true;
    }

    @Override
    public void handle(OutboxEventDO event) {
        log.info("[Outbox][Logging] event: id={}, type={}, aggType={}, aggId={}, tenant={}, body={}",
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getTenantId(),
                event.getEventBody());
    }
}
