package com.bluecone.app.core.cacheinval.coalesce;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoalescerFlushTest {

    @Test
    void shouldFlushOnceWithDeduplicatedKeys() throws Exception {
        List<CacheInvalidationEvent> published = new CopyOnWriteArrayList<>();
        CacheInvalidationPublisher publisher = published::add;

        DefaultInvalidationCoalescer coalescer = new DefaultInvalidationCoalescer(
                publisher,
                Duration.ofMillis(200),
                200,
                50
        );

        CacheInvalidationEvent e1 = new CacheInvalidationEvent(
                "e1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:k1", "1:k2"),
                0L,
                Instant.now()
        );
        CacheInvalidationEvent e2 = new CacheInvalidationEvent(
                "e2",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:k2", "1:k3"),
                0L,
                Instant.now()
        );

        coalescer.submit(e1);
        coalescer.submit(e2);

        TimeUnit.MILLISECONDS.sleep(400);

        assertThat(published).hasSize(1);
        CacheInvalidationEvent merged = published.get(0);
        assertThat(merged.keys()).containsExactlyInAnyOrder("1:k1", "1:k2", "1:k3");
    }

    @Test
    void tooManyKeysShouldTriggerEpochBump() throws Exception {
        List<CacheInvalidationEvent> published = new CopyOnWriteArrayList<>();
        CacheInvalidationPublisher publisher = published::add;

        DefaultInvalidationCoalescer coalescer = new DefaultInvalidationCoalescer(
                publisher,
                Duration.ofMillis(200),
                2,
                50
        );

        CacheInvalidationEvent e1 = new CacheInvalidationEvent(
                "e1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:k1", "1:k2", "1:k3"),
                0L,
                Instant.now()
        );

        coalescer.submit(e1);

        TimeUnit.MILLISECONDS.sleep(400);

        assertThat(published).hasSize(1);
        CacheInvalidationEvent evt = published.get(0);
        assertThat(evt.epochBump()).isTrue();
    }
}

