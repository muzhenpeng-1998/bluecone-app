package com.bluecone.app.core.cacheinval.guard;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StormGuardDecisionTest {

    @Test
    void cntBelowCoalesceShouldDirect() {
        DefaultInvalidationStormGuard guard = new DefaultInvalidationStormGuard(
                5,
                10,
                java.time.Duration.ofMinutes(2),
                50,
                null,
                false
        );
        CacheInvalidationEvent evt = new CacheInvalidationEvent(
                "e1",
                1L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("1:1"),
                0L,
                Instant.now()
        );
        GuardDecision d1 = guard.decide(evt);
        GuardDecision d2 = guard.decide(evt);

        assertThat(d1.decision()).isEqualTo(InvalidationDecision.DIRECT_KEYS);
        assertThat(d2.decision()).isEqualTo(InvalidationDecision.DIRECT_KEYS);
    }

    @Test
    void cntAboveCoalesceShouldCoalesce() {
        DefaultInvalidationStormGuard guard = new DefaultInvalidationStormGuard(
                1,
                100,
                java.time.Duration.ofMinutes(2),
                50,
                null,
                false
        );
        CacheInvalidationEvent evt = new CacheInvalidationEvent(
                "e2",
                2L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("2:1"),
                0L,
                Instant.now()
        );
        GuardDecision d1 = guard.decide(evt);
        GuardDecision d2 = guard.decide(evt);

        assertThat(d1.decision()).isEqualTo(InvalidationDecision.COALESCE);
        assertThat(d2.decision()).isEqualTo(InvalidationDecision.COALESCE);
    }

    @Test
    void stormModeShouldEpochBump() {
        DefaultInvalidationStormGuard guard = new DefaultInvalidationStormGuard(
                1,
                2,
                java.time.Duration.ofMinutes(2),
                50,
                null,
                false
        );
        CacheInvalidationEvent evt = new CacheInvalidationEvent(
            "e3",
            3L,
            InvalidationScope.STORE,
            "store:snap",
            List.of("3:1"),
            0L,
            Instant.now()
        );
        GuardDecision d1 = guard.decide(evt);
        GuardDecision d2 = guard.decide(evt);

        assertThat(d1.decision()).isEqualTo(InvalidationDecision.COALESCE);
        assertThat(d2.decision()).isEqualTo(InvalidationDecision.EPOCH_BUMP);
        assertThat(d2.stormMode()).isTrue();
    }

    @Test
    void keysOverLimitShouldEpochBump() {
        DefaultInvalidationStormGuard guard = new DefaultInvalidationStormGuard(
                100,
                200,
                java.time.Duration.ofMinutes(2),
                1,
                null,
                false
        );
        CacheInvalidationEvent evt = new CacheInvalidationEvent(
                "e4",
                4L,
                InvalidationScope.STORE,
                "store:snap",
                List.of("4:1", "4:2"),
                0L,
                Instant.now()
        );
        GuardDecision d = guard.decide(evt);

        assertThat(d.decision()).isEqualTo(InvalidationDecision.EPOCH_BUMP);
    }
}

