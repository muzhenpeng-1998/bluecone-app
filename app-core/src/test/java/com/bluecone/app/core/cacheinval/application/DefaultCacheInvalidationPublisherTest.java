package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultCacheInvalidationPublisherTest {

    @AfterEach
    void clearTx() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldExecuteAndBroadcastAfterCommit() {
        CacheInvalidationExecutor executor = mock(CacheInvalidationExecutor.class);
        CacheInvalidationBus bus = mock(CacheInvalidationBus.class);
        DefaultCacheInvalidationPublisher publisher = new DefaultCacheInvalidationPublisher(executor, bus);

        CacheInvalidationEvent event = new CacheInvalidationEvent(
                "evt-1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:internal"),
                1L,
                Instant.now()
        );

        TransactionSynchronizationManager.initSynchronization();
        publisher.publishAfterCommit(event);

        // before commit
        verify(executor, never()).execute(any());
        verify(bus, never()).broadcast(any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(executor, times(1)).execute(event);
        verify(bus, times(1)).broadcast(event);
    }
}
