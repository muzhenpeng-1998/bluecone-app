package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.cacheinval.coalesce.InvalidationCoalescer;
import com.bluecone.app.core.cacheinval.guard.GuardDecision;
import com.bluecone.app.core.cacheinval.guard.InvalidationDecision;
import com.bluecone.app.core.cacheinval.guard.InvalidationStormGuard;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublisherEpochBumpTest {

    @Test
    void epochBumpShouldCallEpochProviderAndBroadcastEpochEvent() {
        CacheInvalidationExecutor executor = mock(CacheInvalidationExecutor.class);
        CacheInvalidationBus bus = mock(CacheInvalidationBus.class);
        DefaultCacheInvalidationPublisher delegate = new DefaultCacheInvalidationPublisher(executor, bus);

        InvalidationStormGuard guard = mock(InvalidationStormGuard.class);
        InvalidationCoalescer coalescer = mock(InvalidationCoalescer.class);
        CacheEpochProvider epochProvider = mock(CacheEpochProvider.class);

        when(guard.decide(any())).thenReturn(new GuardDecision(
                InvalidationDecision.EPOCH_BUMP,
                true,
                "storm",
                0L,
                1
        ));
        when(epochProvider.bumpEpoch(1L, "store:snap")).thenReturn(2L);

        ProtectedCacheInvalidationPublisher publisher = new ProtectedCacheInvalidationPublisher(
                delegate,
                guard,
                coalescer,
                epochProvider,
                50
        );

        CacheInvalidationEvent event = new CacheInvalidationEvent(
                "evt-ep",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:internal"),
                1L,
                Instant.now()
        );

        publisher.publishAfterCommit(event);

        verify(epochProvider, times(1)).bumpEpoch(1L, "store:snap");
        verify(executor, never()).execute(any());
        verify(bus, times(1)).broadcast(any(CacheInvalidationEvent.class));
    }
}

