package com.bluecone.app.cacheinval.transport;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple in-process implementation of {@link CacheInvalidationBus}.
 *
 * <p>Suitable for dev or single-instance deployments where cross-node
 * propagation is not required.</p>
 */
public class InProcessCacheInvalidationBus implements CacheInvalidationBus {

    private final List<Consumer<CacheInvalidationEvent>> consumers = new CopyOnWriteArrayList<>();

    @Override
    public void broadcast(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        for (Consumer<CacheInvalidationEvent> consumer : consumers) {
            consumer.accept(event);
        }
    }

    @Override
    public void subscribe(Consumer<CacheInvalidationEvent> consumer) {
        consumers.add(Objects.requireNonNull(consumer, "consumer"));
    }
}

