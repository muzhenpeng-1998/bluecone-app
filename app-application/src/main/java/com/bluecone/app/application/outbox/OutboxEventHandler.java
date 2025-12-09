package com.bluecone.app.application.outbox;

import com.bluecone.app.infra.event.outbox.OutboxEventDO;

/**
 * Handle Outbox events for downstream consumers.
 */
public interface OutboxEventHandler {

    /**
     * Whether this handler is interested in the event.
     */
    boolean supports(OutboxEventDO event);

    /**
     * Handle the event. Exceptions are propagated to the caller.
     */
    void handle(OutboxEventDO event) throws Exception;
}
