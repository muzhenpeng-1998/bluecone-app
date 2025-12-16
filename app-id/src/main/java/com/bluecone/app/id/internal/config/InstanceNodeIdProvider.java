package com.bluecone.app.id.internal.config;

import java.util.Objects;

import org.springframework.core.env.Environment;

/**
 * 实例 nodeId 提供器。
 *
 * <p>用于在启用 long 型 ID（Snowflake）时，为当前实例解析唯一的节点 ID。</p>
 *
 * <p>解析顺序：</p>
 * <ol>
 *     <li>优先使用 {@code bluecone.id.long.node-id} 配置属性；</li>
 *     <li>否则从环境变量 {@value #ENV_BLUECONE_NODE_ID} 读取；</li>
 *     <li>若仍未获取到，则抛出 {@link IllegalStateException} 终止启动。</li>
 * </ol>
 *
 * <p>nodeId 必须在 [0, 1023] 范围内。</p>
 */
public final class InstanceNodeIdProvider {

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
     * 否则尝试从环境变量中解析。</p>
     *
     * @param longId      LongId 配置（可为 null）
     * @param environment Spring 环境
     * @return 解析出的 nodeId，范围 [0, 1023]
     */
    public static long resolveNodeId(BlueconeIdProperties.LongId longId, Environment environment) {
        Objects.requireNonNull(environment, "environment 不能为空");

        Long configuredNodeId = (longId != null ? longId.getNodeId() : null);
        if (configuredNodeId != null) {
            return validateRange(configuredNodeId, "配置属性 bluecone.id.long.node-id");
        }

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

        if (raw == null) {
            throw new IllegalStateException(
                    "已启用 bluecone.id.long.enabled=true 但未配置节点 ID。"
                            + "请通过属性 bluecone.id.long.node-id 或环境变量 "
                            + ENV_BLUECONE_NODE_ID + "（兼容 "
                            + ENV_LEGACY_BLUECONE_ID_NODE_ID + "）提供一个 ["
                            + MIN_NODE_ID + ", " + MAX_NODE_ID + "] 范围内的整数。");
        }

        long parsed;
        try {
            parsed = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(source + " 的值必须是整数，当前为：" + raw, ex);
        }

        return validateRange(parsed, source);
    }

    private static long validateRange(long nodeId, String source) {
        if (nodeId < MIN_NODE_ID || nodeId > MAX_NODE_ID) {
            throw new IllegalStateException(
                    source + " 的值必须在 [" + MIN_NODE_ID + ", " + MAX_NODE_ID + "] 范围内，当前为：" + nodeId);
        }
        return nodeId;
    }
}

