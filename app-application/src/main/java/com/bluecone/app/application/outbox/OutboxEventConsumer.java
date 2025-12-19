package com.bluecone.app.application.outbox;

import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import com.bluecone.app.infra.event.outbox.OutboxEventRepository;
import com.bluecone.app.infra.event.outbox.handler.OutboxEventHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically scans bc_outbox_event, routes each ready event through registered handlers,
 * and updates the status so downstream systems see consumption progress.
 * 
 * Note: 此为旧版本实现，仅在 dev/test 环境使用。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class OutboxEventConsumer {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventHandler> handlers;

    /**
     * Scan at fixed delay so the scheduled task stays lightweight.
     */
    @Scheduled(fixedDelay = 3000L)
    public void consume() {
        List<OutboxEventDO> events = outboxEventRepository.findReadyEvents(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }
        for (OutboxEventDO event : events) {
            try {
                handleEvent(event);
                outboxEventRepository.markSent(event.getId());
            } catch (Exception ex) {
                log.error("Outbox consume failed, id={}, type={}, aggType={}, aggId={}",
                        event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId(), ex);
                int nextRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
                LocalDateTime nextAvailableAt = LocalDateTime.now().plusMinutes(nextRetryCount);
                outboxEventRepository.markFailed(event.getId(), nextRetryCount, nextAvailableAt);
            }
        }
    }

    private void handleEvent(OutboxEventDO event) throws Exception {
        boolean handled = false;
        for (OutboxEventHandler handler : handlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                handled = true;
            }
        }
        if (!handled) {
            log.warn("[Outbox] no handler found for event: id={}, type={}", event.getId(), event.getEventType());
        }
    }
}
