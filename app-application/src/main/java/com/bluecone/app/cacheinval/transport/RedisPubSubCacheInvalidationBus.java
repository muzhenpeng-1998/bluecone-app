package com.bluecone.app.cacheinval.transport;

import com.bluecone.app.config.CacheInvalidationProperties;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub based implementation of {@link CacheInvalidationBus}.
 */
public class RedisPubSubCacheInvalidationBus implements CacheInvalidationBus, MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubCacheInvalidationBus.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheInvalidationProperties properties;
    private final List<Consumer<CacheInvalidationEvent>> consumers = new CopyOnWriteArrayList<>();

    public RedisPubSubCacheInvalidationBus(StringRedisTemplate redisTemplate,
                                           ObjectMapper objectMapper,
                                           CacheInvalidationProperties properties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public void broadcast(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(properties.getRedisTopic(), json);
        } catch (Exception ex) {
            log.warn("[CacheInvalidation] failed to publish Redis message", ex);
        }
    }

    @Override
    public void subscribe(Consumer<CacheInvalidationEvent> consumer) {
        consumers.add(Objects.requireNonNull(consumer, "consumer"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            CacheInvalidationEvent event = objectMapper.readValue(payload, CacheInvalidationEvent.class);
            for (Consumer<CacheInvalidationEvent> consumer : consumers) {
                consumer.accept(event);
            }
        } catch (Exception ex) {
            log.warn("[CacheInvalidation] failed to deserialize Redis message: {}", payload, ex);
        }
    }
}

