package com.bluecone.app.core.idresolve.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 公共 ID 解析配置。
 *
 * <pre>
 * bluecone:
 *   idresolve:
 *     enabled: true
 *     cache:
 *       l1Ttl: PT10M
 *       negativeTtl: PT30S
 *       l2Enabled: true
 *       l2Ttl: PT30M
 *     fallbackToBizTable: false
 *     batchMaxIn: 200
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "bluecone.idresolve")
public class IdResolveProperties {

    /**
     * 是否启用公共 ID 解析能力。
     */
    private boolean enabled = true;

    /**
     * 缓存相关配置。
     */
    private Cache cache = new Cache();

    /**
     * 是否在映射表 miss 时回退到业务主表查询。
     */
    private boolean fallbackToBizTable = false;

    /**
     * 单次批量查询最大 IN 个数。
     */
    private int batchMaxIn = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = (cache != null ? cache : new Cache());
    }

    public boolean isFallbackToBizTable() {
        return fallbackToBizTable;
    }

    public void setFallbackToBizTable(boolean fallbackToBizTable) {
        this.fallbackToBizTable = fallbackToBizTable;
    }

    public int getBatchMaxIn() {
        return batchMaxIn;
    }

    public void setBatchMaxIn(int batchMaxIn) {
        this.batchMaxIn = batchMaxIn;
    }

    public static class Cache {

        /**
         * L1 正向缓存 TTL。
         */
        private Duration l1Ttl = Duration.ofMinutes(10);

        /**
         * L1/L2 负缓存 TTL。
         */
        private Duration negativeTtl = Duration.ofSeconds(30);

        /**
         * 是否启用 L2 Redis 缓存。
         */
        private boolean l2Enabled = true;

        /**
         * L2 正向缓存 TTL。
         */
        private Duration l2Ttl = Duration.ofMinutes(30);

        /**
         * L1 最大条目数。
         */
        private long l1MaxSize = 100_000L;

        public Duration getL1Ttl() {
            return l1Ttl;
        }

        public void setL1Ttl(Duration l1Ttl) {
            this.l1Ttl = (l1Ttl != null ? l1Ttl : Duration.ofMinutes(10));
        }

        public Duration getNegativeTtl() {
            return negativeTtl;
        }

        public void setNegativeTtl(Duration negativeTtl) {
            this.negativeTtl = (negativeTtl != null ? negativeTtl : Duration.ofSeconds(30));
        }

        public boolean isL2Enabled() {
            return l2Enabled;
        }

        public void setL2Enabled(boolean l2Enabled) {
            this.l2Enabled = l2Enabled;
        }

        public Duration getL2Ttl() {
            return l2Ttl;
        }

        public void setL2Ttl(Duration l2Ttl) {
            this.l2Ttl = (l2Ttl != null ? l2Ttl : Duration.ofMinutes(30));
        }

        public long getL1MaxSize() {
            return l1MaxSize;
        }

        public void setL1MaxSize(long l1MaxSize) {
            this.l1MaxSize = l1MaxSize;
        }
    }
}

