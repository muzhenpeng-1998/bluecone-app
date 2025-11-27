package com.bluecone.app.infra.cache.consistency;

import com.bluecone.app.infra.cache.core.CacheKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub 实现的一致性总线，关注“失效事件”的单一语义。
 */
public class RedisConsistencyBus implements ConsistencyBus, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RedisConsistencyBus.class);
    private static final String CHANNEL = "bluecone:cache:invalidate";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer listenerContainer;
    private final List<Consumer<CacheKey>> listeners = new CopyOnWriteArrayList<>();

    public RedisConsistencyBus(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.listenerContainer = new RedisMessageListenerContainer();
        Assert.notNull(redisTemplate.getConnectionFactory(), "RedisConnectionFactory must not be null");
        this.listenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
    }

    @Override
    public void publishInvalidation(CacheKey key, String reason) {
        InvalidationMessage message = InvalidationMessage.from(key, reason);
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            log.warn("consistency.publish failed, key={}, message={}", key, ex.getMessage());
        }
    }

    @Override
    public void registerInvalidationListener(Consumer<CacheKey> listener) {
        listeners.add(listener);
    }

    @Override
    public void afterPropertiesSet() {
        MessageListener adapter = this::onMessage;
        listenerContainer.addMessageListener(adapter, new ChannelTopic(CHANNEL));
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();
        log.info("consistency.bus redis listener started on channel={}", CHANNEL);
    }

    @Override
    public void destroy() {
        listenerContainer.stop();
    }

    private void onMessage(Message message, byte[] pattern) {
        try {
            InvalidationMessage payload = objectMapper.readValue(message.getBody(), InvalidationMessage.class);
            CacheKey key = payload.toCacheKey();
            listeners.forEach(listener -> listener.accept(key));
        } catch (IOException ex) {
            log.warn("consistency.bus decode failed: {}", ex.getMessage());
        }
    }

    private record InvalidationMessage(String tenantId, String domain, String bizId, String version, String reason) {
        static InvalidationMessage from(CacheKey key, String reason) {
            return new InvalidationMessage(key.getTenantId(), key.getDomain(), key.getBizId(), key.getVersion().orElse(null), reason);
        }

        CacheKey toCacheKey() {
            CacheKey base = CacheKey.generic(tenantId, domain, bizId);
            return version == null ? base : base.withVersion(version);
        }
    }
}
