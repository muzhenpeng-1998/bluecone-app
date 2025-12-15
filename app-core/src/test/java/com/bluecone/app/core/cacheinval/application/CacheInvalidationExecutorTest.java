package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.contextkit.CacheKey;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.CacheValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CacheInvalidationExecutorTest {

    @Test
    void shouldInvalidateProvidedKeys() {
        ContextCache cache = mock(ContextCache.class);
        DefaultCacheInvalidationExecutor executor = new DefaultCacheInvalidationExecutor(cache, null);
        CacheInvalidationEvent event = new CacheInvalidationEvent(
                "evt-1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:internalId"),
                10L,
                Instant.now()
        );

        executor.execute(event);

        verify(cache).invalidate(eq(new CacheKey("store:snap", "1:internalId")));
    }

    @Test
    void shouldBeNoOpForEmptyKeys() {
        ContextCache cache = mock(ContextCache.class);
        DefaultCacheInvalidationExecutor executor = new DefaultCacheInvalidationExecutor(cache, null);
        CacheInvalidationEvent event = new CacheInvalidationEvent(
                "evt-2",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of(),
                0L,
                Instant.now()
        );

        executor.execute(event);

        verifyNoInteractions(cache);
    }
}
