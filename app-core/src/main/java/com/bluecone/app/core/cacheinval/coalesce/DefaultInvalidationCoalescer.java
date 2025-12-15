package com.bluecone.app.core.cacheinval.coalesce;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.cacheinval.guard.InvalidationDecision;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation that groups events by tenant+namespace in a debounce window.
 *
 * <p>Flushes merged events via a delegate {@link CacheInvalidationPublisher}.
 * To avoid recursive coalescing, a special flag is used on synthetic events.</p>
 */
public class DefaultInvalidationCoalescer implements InvalidationCoalescer {

    private static final String META_COALESCED = "COALESCED";

    private final CacheInvalidationPublisher publisher;
    private final Duration debounceWindow;
    private final int maxKeysPerBatch;
    private final int maxKeysPerEvent;

    private final ScheduledExecutorService scheduler;

    private final Map<GroupKey, PendingBatch> batches = new ConcurrentHashMap<>();

    public DefaultInvalidationCoalescer(CacheInvalidationPublisher publisher,
                                        Duration debounceWindow,
                                        int maxKeysPerBatch,
                                        int maxKeysPerEvent) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.debounceWindow = Objects.requireNonNull(debounceWindow, "debounceWindow");
        this.maxKeysPerBatch = maxKeysPerBatch;
        this.maxKeysPerEvent = maxKeysPerEvent;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-inval-coalescer");
            t.setDaemon(true);
            return t;
        });
        long interval = Math.max(50L, debounceWindow.toMillis() / 2);
        scheduler.scheduleAtFixedRate(this::flushDueBatches, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void submit(CacheInvalidationEvent evt) {
        if (evt == null || evt.keys() == null || evt.keys().isEmpty()) {
            return;
        }
        if (META_COALESCED.equals(evt.eventId())) {
            // already coalesced, do not re-coalesce
            publisher.publishAfterCommit(evt);
            return;
        }
        GroupKey key = new GroupKey(evt.tenantId(), evt.namespace(), evt.scope());
        batches.compute(key, (k, existing) -> {
            if (existing == null) {
                existing = new PendingBatch();
            }
            existing.add(evt);
            return existing;
        });
    }

    private void flushDueBatches() {
        Instant now = Instant.now();
        for (Map.Entry<GroupKey, PendingBatch> entry : batches.entrySet()) {
            GroupKey key = entry.getKey();
            PendingBatch batch = entry.getValue();
            if (batch == null) {
                continue;
            }
            if (batch.isDue(now, debounceWindow) || batch.keys.size() >= maxKeysPerBatch) {
                batches.remove(key, batch);
                flushBatch(key, batch);
            }
        }
    }

    private void flushBatch(GroupKey key, PendingBatch batch) {
        if (batch.keys.isEmpty()) {
            return;
        }
        if (batch.keys.size() > maxKeysPerBatch) {
            // too many keys, downgrade to epoch bump via special event
            CacheInvalidationEvent epochEvt = new CacheInvalidationEvent(
                    UUID.randomUUID().toString(),
                    key.tenantId,
                    key.scope,
                    key.namespace,
                    List.of(),
                    0L,
                    Instant.now(),
                    true,
                    null,
                    InvalidationDecision.EPOCH_BUMP.name()
            );
            publisher.publishAfterCommit(epochEvt);
            return;
        }

        List<String> mergedKeys = List.copyOf(batch.keys);
        int totalKeys = mergedKeys.size();
        if (totalKeys <= maxKeysPerEvent) {
            CacheInvalidationEvent mergedEvt = new CacheInvalidationEvent(
                    META_COALESCED,
                    key.tenantId,
                    key.scope,
                    key.namespace,
                    mergedKeys,
                    0L,
                    Instant.now(),
                    false,
                    null,
                    InvalidationDecision.COALESCE.name()
            );
            publisher.publishAfterCommit(mergedEvt);
        } else {
            int from = 0;
            while (from < totalKeys) {
                int to = Math.min(from + maxKeysPerEvent, totalKeys);
                List<String> subList = mergedKeys.subList(from, to);
                CacheInvalidationEvent partEvt = new CacheInvalidationEvent(
                        META_COALESCED,
                        key.tenantId,
                        key.scope,
                        key.namespace,
                        List.copyOf(subList),
                        0L,
                        Instant.now(),
                        false,
                        null,
                        InvalidationDecision.COALESCE.name()
                );
                publisher.publishAfterCommit(partEvt);
                from = to;
            }
        }
    }

    private static final class GroupKey {
        private final long tenantId;
        private final String namespace;
        private final InvalidationScope scope;

        private GroupKey(long tenantId, String namespace, InvalidationScope scope) {
            this.tenantId = tenantId;
            this.namespace = namespace;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupKey that)) return false;
            if (tenantId != that.tenantId) return false;
            if (!Objects.equals(namespace, that.namespace)) return false;
            return scope == that.scope;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(tenantId);
            result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
            result = 31 * result + (scope != null ? scope.hashCode() : 0);
            return result;
        }
    }

    private static final class PendingBatch {
        private final Set<String> keys = new LinkedHashSet<>();
        private Instant firstEventTime;

        void add(CacheInvalidationEvent evt) {
            if (firstEventTime == null) {
                firstEventTime = evt.occurredAt() != null ? evt.occurredAt() : Instant.now();
            }
            if (evt.keys() != null) {
                keys.addAll(evt.keys());
            }
        }

        boolean isDue(Instant now, Duration window) {
            if (firstEventTime == null) {
                return false;
            }
            return firstEventTime.plus(window).isBefore(now) || firstEventTime.plus(window).equals(now);
        }
    }
}

