package com.bluecone.app.core.cacheinval.observability;

import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogEntry;
import com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogWriter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for CacheInvalidationLogWriter interface.
 * 
 * <p>Note: This test does not depend on CacheInvalidationListener from app-application
 * to avoid circular dependencies. It only tests the writer interface directly.</p>
 */
class CacheInvalidationLogWriterTest {

    @Test
    void writerShouldAcceptLogEntry() {
        AtomicReference<CacheInvalidationLogEntry> ref = new AtomicReference<>();
        CacheInvalidationLogWriter writer = ref::set;

        // Create a log entry directly (simulating what the listener would create)
        CacheInvalidationLogEntry entry = new CacheInvalidationLogEntry(
                Instant.now(),
                Instant.now(),
                1L,
                "STORE",
                "store:snap",
                "evt-xyz",
                1,
                List.of("a1b2c3d4"),
                10L,
                "OUTBOX",
                "instance-1",
                "OK",
                null,
                null,
                false,
                null
        );
        
        writer.write(entry);

        assertThat(ref.get()).isNotNull();
        assertThat(ref.get().keySampleHashes()).containsExactly("a1b2c3d4");
        assertThat(ref.get().scope()).isEqualTo("STORE");
        assertThat(ref.get().namespace()).isEqualTo("store:snap");
        assertThat(ref.get().eventId()).isEqualTo("evt-xyz");
    }
    
    @Test
    void writerShouldHandleMultipleKeys() {
        AtomicReference<CacheInvalidationLogEntry> ref = new AtomicReference<>();
        CacheInvalidationLogWriter writer = ref::set;

        CacheInvalidationLogEntry entry = new CacheInvalidationLogEntry(
                Instant.now(),
                Instant.now(),
                2L,
                "INVENTORY",
                "inv:snap",
                "evt-abc",
                3,
                List.of("hash1", "hash2", "hash3"),
                15L,
                "REDIS_PUBSUB",
                "instance-2",
                "OK",
                null,
                null,
                false,
                null
        );
        
        writer.write(entry);

        assertThat(ref.get()).isNotNull();
        assertThat(ref.get().keySampleHashes()).hasSize(3);
        assertThat(ref.get().keysCount()).isEqualTo(3);
    }
}
