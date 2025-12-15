package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.coalesce.InvalidationCoalescer;
import com.bluecone.app.core.cacheinval.guard.GuardDecision;
import com.bluecone.app.core.cacheinval.guard.InvalidationDecision;
import com.bluecone.app.core.cacheinval.guard.InvalidationStormGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * CacheInvalidationPublisher decorator that applies storm protection:
 * - decides DIRECT_KEYS / COALESCE / EPOCH_BUMP via {@link InvalidationStormGuard};
 * - performs epoch bump via {@link CacheEpochProvider} when needed;
 * - delegates coalesced events to {@link InvalidationCoalescer}.
 */
public class ProtectedCacheInvalidationPublisher implements CacheInvalidationPublisher {

    private final DefaultCacheInvalidationPublisher delegate;
    private final InvalidationStormGuard stormGuard;
    private final InvalidationCoalescer coalescer;
    private final CacheEpochProvider epochProvider;
    private final int maxKeysPerEvent;

    public ProtectedCacheInvalidationPublisher(DefaultCacheInvalidationPublisher delegate,
                                               InvalidationStormGuard stormGuard,
                                               InvalidationCoalescer coalescer,
                                               CacheEpochProvider epochProvider,
                                               int maxKeysPerEvent) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.stormGuard = Objects.requireNonNull(stormGuard, "stormGuard");
        this.coalescer = Objects.requireNonNull(coalescer, "coalescer");
        this.epochProvider = Objects.requireNonNull(epochProvider, "epochProvider");
        this.maxKeysPerEvent = maxKeysPerEvent;
    }

    @Override
    public void publishAfterCommit(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        GuardDecision decision = stormGuard.decide(event);
        InvalidationDecision d = decision.decision();

        // Ensure protection hint is carried for observability
        CacheInvalidationEvent baseEvent = withProtectionHint(event, d, null);

        if (d == InvalidationDecision.DIRECT_KEYS) {
            delegate.publishAfterCommit(baseEvent);
            return;
        }
        if (d == InvalidationDecision.COALESCE) {
            coalescer.submit(baseEvent);
            return;
        }

        if (d == InvalidationDecision.EPOCH_BUMP) {
            long newEpoch = epochProvider.bumpEpoch(event.tenantId(), event.namespace());
            CacheInvalidationEvent epochEvt = new CacheInvalidationEvent(
                    event.eventId(),
                    event.tenantId(),
                    event.scope(),
                    event.namespace(),
                    List.of(),
                    event.configVersion(),
                    event.occurredAt() != null ? event.occurredAt() : Instant.now(),
                    true,
                    newEpoch,
                    InvalidationDecision.EPOCH_BUMP.name()
            );
            delegate.publishAfterCommit(epochEvt);
        }
    }

    private CacheInvalidationEvent withProtectionHint(CacheInvalidationEvent event,
                                                      InvalidationDecision decision,
                                                      Long newEpoch) {
        return new CacheInvalidationEvent(
                event.eventId(),
                event.tenantId(),
                event.scope(),
                event.namespace(),
                event.keys(),
                event.configVersion(),
                event.occurredAt(),
                event.epochBump(),
                newEpoch != null ? newEpoch : event.newEpoch(),
                decision != null ? decision.name() : event.protectionHint()
        );
    }
}

