package com.bluecone.app.cacheinval.transport;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InProcessCacheInvalidationBusTest {

    @Test
    void broadcastShouldNotifyAllSubscribers() {
        InProcessCacheInvalidationBus bus = new InProcessCacheInvalidationBus();
        AtomicInteger counter = new AtomicInteger();

        bus.subscribe(evt -> counter.incrementAndGet());
        bus.subscribe(evt -> counter.incrementAndGet());

        CacheInvalidationEvent event = new CacheInvalidationEvent(
                "evt-1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:internal"),
                1L,
                Instant.now()
        );

        bus.broadcast(event);

        assertThat(counter.get()).isEqualTo(2);
    }
}
