package com.bluecone.app.infra.cache.consistency;

import com.bluecone.app.infra.cache.core.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 兼容无 Redis 的环境：仅在单节点内工作，不广播失效。
 */
public class NoopConsistencyBus implements ConsistencyBus {

    private static final Logger log = LoggerFactory.getLogger(NoopConsistencyBus.class);

    @Override
    public void publishInvalidation(CacheKey key, String reason) {
        log.debug("consistency.bus.noop publish ignored key={} reason={}", key, reason);
    }

    @Override
    public void registerInvalidationListener(Consumer<CacheKey> listener) {
        // 单机模式无需注册远程事件
    }
}
