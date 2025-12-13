package com.bluecone.app.id.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BlueCone ID 模块配置属性。
 *
 * <p>通过 {@code bluecone.id.*} 前缀进行配置。</p>
 */
@ConfigurationProperties(prefix = "bluecone.id")
public class BlueconeIdProperties {

    /**
     * 是否启用 ID 生成模块。
     */
    private boolean enabled = true;

    /**
     * ULID 相关配置。
     */
    private Ulid ulid = new Ulid();

    /**
     * Snowflake long ID 相关配置。
     */
    private LongId longId = new LongId();

    /**
     * 对外公开 ID（PublicId）配置。
     */
    private PublicId publicId = new PublicId();

    /**
     * MyBatis 相关配置。
     */
    private Mybatis mybatis = new Mybatis();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Ulid getUlid() {
        return ulid;
    }

    public void setUlid(Ulid ulid) {
        // 避免为 null，保证后续使用安全
        this.ulid = (ulid != null ? ulid : new Ulid());
    }

    /**
     * Snowflake long ID 相关配置。
     */
    public LongId getLong() {
        return longId;
    }

    public void setLong(LongId longId) {
        this.longId = (longId != null ? longId : new LongId());
    }

    public PublicId getPublicId() {
        return publicId;
    }

    public void setPublicId(PublicId publicId) {
        this.publicId = (publicId != null ? publicId : new PublicId());
    }

    public Mybatis getMybatis() {
        return mybatis;
    }

    public void setMybatis(Mybatis mybatis) {
        this.mybatis = (mybatis != null ? mybatis : new Mybatis());
    }

    /**
     * ULID 配置。
     */
    public static class Ulid {

        /**
         * 是否启用 ULID 生成能力。
         */
        private boolean enabled = true;

        /**
         * ULID 生成模式：STRICT（单锁全序）或 STRIPED（多分片以提升并发）。
         */
        private Mode mode = Mode.STRIPED;

        /**
         * 条带数量，默认不超过 32 与 CPU 核心数。
         */
        private int stripes = Math.min(32, Runtime.getRuntime().availableProcessors());

        /**
         * 是否启用 Micrometer 指标采集。
         */
        private boolean metricsEnabled = true;

        /**
         * 时钟回拨处理策略配置。
         */
        private Rollback rollback = new Rollback();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = (mode != null ? mode : Mode.STRIPED);
        }

        public int getStripes() {
            return stripes;
        }

        public void setStripes(int stripes) {
            this.stripes = stripes;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public Rollback getRollback() {
            return rollback;
        }

        public void setRollback(Rollback rollback) {
            this.rollback = (rollback != null ? rollback : new Rollback());
        }

        /**
         * 时钟回拨处理配置。
         */
        public static class Rollback {

            /**
             * 回拨处理策略，默认不中断业务（USE_LAST）。
             */
            private Policy policy = Policy.USE_LAST;

            /**
             * FAIL_FAST 策略下，当回拨幅度超过该阈值（毫秒）时抛出异常。
             */
            private long failFastThresholdMs = 5000L;

            /**
             * WAIT 策略下的最大等待毫秒数。
             */
            private long waitMaxMs = 50L;

            public Policy getPolicy() {
                return policy;
            }

            public void setPolicy(Policy policy) {
                this.policy = (policy != null ? policy : Policy.USE_LAST);
            }

            public long getFailFastThresholdMs() {
                return failFastThresholdMs;
            }

            public void setFailFastThresholdMs(long failFastThresholdMs) {
                this.failFastThresholdMs = failFastThresholdMs;
            }

            public long getWaitMaxMs() {
                return waitMaxMs;
            }

            public void setWaitMaxMs(long waitMaxMs) {
                this.waitMaxMs = waitMaxMs;
            }
        }

        /**
         * 时钟回拨处理策略。
         */
        public enum Policy {

            /**
             * 使用上一次时间戳继续生成，保证单调，不中断业务。
             */
            USE_LAST,

            /**
             * 等待一小段时间后重试，期望系统时钟恢复。
             */
            WAIT,

            /**
             * 回拨幅度过大时快速失败，抛出异常。
             */
            FAIL_FAST
        }
    }

    /**
     * ULID 生成模式。
     */
    public enum Mode {

        /**
         * 严格单序模式，单锁保证全局顺序。
         */
        STRICT,

        /**
         * 条带化模式，多分片状态以降低锁竞争。
         */
        STRIPED
    }

    /**
     * PublicId 相关配置。
     */
    public static class PublicId {

        /**
         * 是否启用 PublicId 能力。
         */
        private boolean enabled = true;

        /**
         * 类型前缀与载荷之间的分隔符，默认为下划线。
         */
        private String separator = "_";

        /**
         * 是否将 ULID/Base32 载荷转换为小写形式。
         */
        private boolean lowerCase = true;

        /**
         * 编码格式，默认使用 ULID 的 Base32 字符串形式。
         */
        private Format format = Format.ULID_BASE32;

        /**
         * 是否启用校验和（用于人工录入/防篡改场景）。
         */
        private boolean checksumEnabled = false;

        /**
         * 校验和使用的字节数，目前仅支持 1 字节（CRC8），预留扩展。
         */
        private int checksumBytes = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

        public boolean isLowerCase() {
            return lowerCase;
        }

        public void setLowerCase(boolean lowerCase) {
            this.lowerCase = lowerCase;
        }

        public Format getFormat() {
            return format;
        }

        public void setFormat(Format format) {
            this.format = (format != null ? format : Format.ULID_BASE32);
        }

        public boolean isChecksumEnabled() {
            return checksumEnabled;
        }

        public void setChecksumEnabled(boolean checksumEnabled) {
            this.checksumEnabled = checksumEnabled;
        }

        public int getChecksumBytes() {
            return checksumBytes;
        }

        public void setChecksumBytes(int checksumBytes) {
            this.checksumBytes = checksumBytes;
        }

        /**
         * PublicId 编码格式。
         */
        public enum Format {

            /**
             * 使用 ULID 标准 Base32 字符串作为载荷。
             */
            ULID_BASE32,

            /**
             * 将 128 位 ULID 二进制表示编码为固定长度 Base62 字符串。
             */
            BASE62_128
        }
    }

    /**
     * MyBatis 相关配置。
     */
    public static class Mybatis {

        /**
         * 是否启用 MyBatis 相关自动配置（TypeHandler 等）。
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Snowflake long ID 相关配置。
     */
    public static class LongId {

        /**
         * 是否启用 long 型 ID 生成。
         */
        private boolean enabled = false;

        /**
         * 节点 ID，范围 [0, 1023]。
         *
         * <p>当 {@code bluecone.id.long.enabled=true} 时，必须为当前实例提供唯一的 nodeId：</p>
         * <ul>
         *     <li>推荐通过配置属性 {@code bluecone.id.long.node-id} 明确配置；</li>
         *     <li>也可以省略该属性，改为在运行环境中注入 {@code BLUECONE_NODE_ID}
         *     （兼容 {@code BLUECONE_ID_NODE_ID}），由 {@link InstanceNodeIdProvider} 解析。</li>
         * </ul>
         *
         * <p>若启用了 long ID 但既未配置属性、也未提供环境变量，则应用启动会失败，以避免隐性 nodeId 撞号。</p>
         */
        private Long nodeId;

        /**
         * 自定义纪元毫秒。
         */
        private long epochMillis = 0L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Long getNodeId() {
            return nodeId;
        }

        public void setNodeId(Long nodeId) {
            this.nodeId = nodeId;
        }

        public long getEpochMillis() {
            return epochMillis;
        }

        public void setEpochMillis(long epochMillis) {
            this.epochMillis = epochMillis;
        }
    }
}
