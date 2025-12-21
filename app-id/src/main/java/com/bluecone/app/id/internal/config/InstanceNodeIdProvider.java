package com.bluecone.app.id.internal.config;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Objects;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * 实例 nodeId 提供器。
 *
 * <p>用于在启用 long 型 ID（Snowflake）时，为当前实例解析唯一的节点 ID。</p>
 *
 * <p>解析顺序：</p>
 * <ol>
 *     <li>优先使用 {@code bluecone.id.long.node-id} 配置属性；</li>
 *     <li>否则从环境变量 {@value #ENV_BLUECONE_NODE_ID} 读取（兼容 {@value #ENV_LEGACY_BLUECONE_ID_NODE_ID}）；</li>
 *     <li>若仍未获取到，则基于主机信息派生一个 nodeId（适用于单实例/少量实例场景）。</li>
 * </ol>
 *
 * <p><b>重要提示：</b>派生的 nodeId 不能保证多实例绝对不冲突，
 * 生产环境多实例部署时必须显式配置 nodeId。</p>
 *
 * <p>nodeId 必须在 [0, 1023] 范围内。</p>
 */
public final class InstanceNodeIdProvider {

    private static final Logger log = LoggerFactory.getLogger(InstanceNodeIdProvider.class);

    /**
     * Snowflake 中 nodeId 的最小值。
     */
    public static final long MIN_NODE_ID = 0L;

    /**
     * Snowflake 中 nodeId 的最大值（10 bit，对应 [0,1023]）。
     */
    public static final long MAX_NODE_ID = (1L << 10) - 1L;

    /**
     * 默认使用的环境变量名称。
     */
    public static final String ENV_BLUECONE_NODE_ID = "BLUECONE_NODE_ID";

    /**
     * 兼容旧文档中提到的环境变量名称。
     */
    public static final String ENV_LEGACY_BLUECONE_ID_NODE_ID = "BLUECONE_ID_NODE_ID";

    private InstanceNodeIdProvider() {
    }

    /**
     * 解析当前实例的 nodeId。
     *
     * <p>当 {@code longId} 不为 null 且配置了 {@code nodeId} 时，优先使用配置值；
     * 否则尝试从环境变量中解析；若都未配置，则派生一个 nodeId。</p>
     *
     * @param longId      LongId 配置（可为 null）
     * @param environment Spring 环境
     * @return 解析出的 nodeId，范围 [0, 1023]
     */
    public static long resolveNodeId(BlueconeIdProperties.LongId longId, Environment environment) {
        Objects.requireNonNull(environment, "environment 不能为空");

        // 1. 优先使用配置属性
        Long configuredNodeId = (longId != null ? longId.getNodeId() : null);
        if (configuredNodeId != null) {
            long nodeId = validateRange(configuredNodeId, "配置属性 bluecone.id.long.node-id");
            log.info("使用配置的 nodeId: {}", nodeId);
            return nodeId;
        }

        // 2. 尝试从环境变量读取
        String raw = null;
        String source = null;

        String value = environment.getProperty(ENV_BLUECONE_NODE_ID);
        if (value != null && !value.trim().isEmpty()) {
            raw = value.trim();
            source = "环境变量 " + ENV_BLUECONE_NODE_ID;
        } else {
            value = environment.getProperty(ENV_LEGACY_BLUECONE_ID_NODE_ID);
            if (value != null && !value.trim().isEmpty()) {
                raw = value.trim();
                source = "环境变量 " + ENV_LEGACY_BLUECONE_ID_NODE_ID;
            }
        }

        if (raw != null) {
            long parsed;
            try {
                parsed = Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(source + " 的值必须是整数，当前为：" + raw, ex);
            }
            long nodeId = validateRange(parsed, source);
            log.info("使用环境变量的 nodeId: {} (来源: {})", nodeId, source);
            return nodeId;
        }

        // 3. 派生 nodeId（兜底方案）
        long derivedNodeId = deriveNodeId(environment);
        log.warn("未配置 nodeId，已派生 nodeId: {}。" +
                "注意：派生的 nodeId 不能保证多实例绝对不冲突，" +
                "生产环境多实例部署时请通过 bluecone.id.long.node-id 或环境变量 {} 显式配置。",
                derivedNodeId, ENV_BLUECONE_NODE_ID);
        return derivedNodeId;
    }

    /**
     * 派生 nodeId（基于主机信息）。
     *
     * <p>派生种子优先级：
     * <ol>
     *   <li>WECHAT_CLOUD_RUN_INSTANCE_ID（微信云托管实例 ID）</li>
     *   <li>POD_NAME（Kubernetes Pod 名称）</li>
     *   <li>HOSTNAME（主机名）</li>
     *   <li>pid@hostname（进程 ID + 主机名）</li>
     * </ol>
     *
     * @param environment Spring 环境
     * @return 派生的 nodeId，范围 [0, 1023]
     */
    private static long deriveNodeId(Environment environment) {
        String seed = null;

        // 优先使用微信云托管实例 ID
        seed = environment.getProperty("WECHAT_CLOUD_RUN_INSTANCE_ID");
        if (seed != null && !seed.trim().isEmpty()) {
            log.debug("使用 WECHAT_CLOUD_RUN_INSTANCE_ID 作为派生种子: {}", seed);
            return hashToNodeId(seed.trim());
        }

        // 尝试 Kubernetes Pod 名称
        seed = environment.getProperty("POD_NAME");
        if (seed != null && !seed.trim().isEmpty()) {
            log.debug("使用 POD_NAME 作为派生种子: {}", seed);
            return hashToNodeId(seed.trim());
        }

        // 尝试主机名
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.isEmpty()) {
                log.debug("使用 HOSTNAME 作为派生种子: {}", hostname);
                return hashToNodeId(hostname);
            }
        } catch (Exception e) {
            log.debug("无法获取主机名: {}", e.getMessage());
        }

        // 兜底：使用 pid@hostname
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        log.debug("使用 pid@hostname 作为派生种子: {}", runtimeName);
        return hashToNodeId(runtimeName);
    }

    /**
     * 将字符串哈希到 [0, 1023] 范围内。
     *
     * @param seed 种子字符串
     * @return nodeId，范围 [0, 1023]
     */
    private static long hashToNodeId(String seed) {
        CRC32 crc32 = new CRC32();
        crc32.update(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long hash = crc32.getValue();
        return hash & MAX_NODE_ID; // 取低 10 位
    }

    private static long validateRange(long nodeId, String source) {
        if (nodeId < MIN_NODE_ID || nodeId > MAX_NODE_ID) {
            throw new IllegalStateException(
                    source + " 的值必须在 [" + MIN_NODE_ID + ", " + MAX_NODE_ID + "] 范围内，当前为：" + nodeId);
        }
        return nodeId;
    }
}

