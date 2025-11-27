package com.bluecone.app.infra.redis.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.redis.support.RedisEnvProvider;
import com.bluecone.app.infra.redis.support.RedisTenantProvider;

/**
 * 生成标准化的 Redis key，格式：
 * <p>bluecone:{env}:{tenantId}:{namespace}:{bizId}:{attributes...}</p>
 * <ul>
 *     <li>env：环境隔离（dev/test/stage/prod）</li>
 *     <li>tenantId：租户隔离，全局 key 不带此段</li>
 *     <li>namespace：业务命名空间，由 {@link RedisKeyNamespace} 定义</li>
 *     <li>bizId：业务标识，如 orderId/userId 等</li>
 *     <li>attributes：可选后缀，用于区分变体</li>
 * </ul>
 */
@Component
public class RedisKeyBuilder {

    private static final String PLATFORM_PREFIX = "bluecone";

    private final RedisEnvProvider envProvider;
    private final RedisTenantProvider tenantProvider;

    public RedisKeyBuilder(RedisEnvProvider envProvider, RedisTenantProvider tenantProvider) {
        this.envProvider = envProvider;
        this.tenantProvider = tenantProvider;
    }

    /**
     * 构建带环境与租户前缀的租户级 key。
     *
     * @param namespace  业务命名空间
     * @param bizId      业务标识，如 orderId/sessionId
     * @param attributes 可选后缀，使用冒号拼接
     * @return 标准化 Redis key
     */
    public String build(RedisKeyNamespace namespace, String bizId, String... attributes) {
        Assert.notNull(namespace, "namespace must not be null");
        Assert.hasText(bizId, "bizId must not be blank");
        String prefix = String.format("%s:%s:%d:%s:", PLATFORM_PREFIX, envProvider.getEnv(),
                tenantProvider.getTenantIdOrDefault(), namespace.code());
        return prefix + appendAttributes(bizId, attributes);
    }

    /**
     * 构建不带租户前缀的全局 key，适用于跨租户系统级配置。
     *
     * @param namespace  业务命名空间
     * @param bizId      业务标识
     * @param attributes 可选后缀，使用冒号拼接
     * @return 标准化全局 Redis key
     */
    public String buildForGlobal(RedisKeyNamespace namespace, String bizId, String... attributes) {
        Assert.notNull(namespace, "namespace must not be null");
        Assert.hasText(bizId, "bizId must not be blank");
        String prefix = String.format("%s:%s:%s:", PLATFORM_PREFIX, envProvider.getEnv(), namespace.code());
        return prefix + appendAttributes(bizId, attributes);
    }

    private String appendAttributes(String bizId, String... attributes) {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(bizId);
        if (attributes != null && attributes.length > 0) {
            Arrays.stream(attributes)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(Objects::nonNull)
                    .forEach(joiner::add);
        }
        return joiner.toString();
    }
}
