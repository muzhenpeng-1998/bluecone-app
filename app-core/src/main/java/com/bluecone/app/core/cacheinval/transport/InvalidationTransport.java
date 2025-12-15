package com.bluecone.app.core.cacheinval.transport;

/**
 * Transport mechanisms for distributing cache invalidation events.
 */
public enum InvalidationTransport {

    OUTBOX,

    REDIS_PUBSUB,

    INPROCESS
}

