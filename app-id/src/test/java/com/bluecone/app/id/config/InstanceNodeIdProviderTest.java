package com.bluecone.app.id.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.internal.config.InstanceNodeIdProvider;

/**
 * InstanceNodeIdProvider 行为测试。
 */
class InstanceNodeIdProviderTest {

    @Test
    void shouldPreferConfiguredPropertyWhenPresent() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        longId.setNodeId(10L);
        MockEnvironment env = new MockEnvironment()
                .withProperty(InstanceNodeIdProvider.ENV_BLUECONE_NODE_ID, "1");

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertEquals(10L, nodeId);
    }

    @Test
    void shouldReadFromNewEnvironmentVariableWhenPropertyMissing() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment()
                .withProperty(InstanceNodeIdProvider.ENV_BLUECONE_NODE_ID, "5");

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertEquals(5L, nodeId);
    }

    @Test
    void shouldFallbackToLegacyEnvironmentVariable() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment()
                .withProperty(InstanceNodeIdProvider.ENV_LEGACY_BLUECONE_ID_NODE_ID, "7");

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertEquals(7L, nodeId);
    }

    /**
     * 未配置 nodeId 时应派生一个合法的 nodeId（0~1023）。
     */
    @Test
    void shouldDeriveNodeIdWhenNoConfigured() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment();

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        // 派生的 nodeId 应在合法范围内
        assertTrue(nodeId >= InstanceNodeIdProvider.MIN_NODE_ID);
        assertTrue(nodeId <= InstanceNodeIdProvider.MAX_NODE_ID);
    }

    /**
     * 派生的 nodeId 应稳定（同一环境多次调用返回相同值）。
     */
    @Test
    void derivedNodeIdShouldBeStable() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment();

        long nodeId1 = InstanceNodeIdProvider.resolveNodeId(longId, env);
        long nodeId2 = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertEquals(nodeId1, nodeId2);
    }

    /**
     * 使用 WECHAT_CLOUD_RUN_INSTANCE_ID 派生 nodeId。
     */
    @Test
    void shouldDeriveFromWechatCloudRunInstanceId() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment()
                .withProperty("WECHAT_CLOUD_RUN_INSTANCE_ID", "instance-12345");

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertTrue(nodeId >= InstanceNodeIdProvider.MIN_NODE_ID);
        assertTrue(nodeId <= InstanceNodeIdProvider.MAX_NODE_ID);
    }

    /**
     * 使用 POD_NAME 派生 nodeId。
     */
    @Test
    void shouldDeriveFromPodName() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment()
                .withProperty("POD_NAME", "bluecone-app-7d9f8c5b4-xyz");

        long nodeId = InstanceNodeIdProvider.resolveNodeId(longId, env);

        assertTrue(nodeId >= InstanceNodeIdProvider.MIN_NODE_ID);
        assertTrue(nodeId <= InstanceNodeIdProvider.MAX_NODE_ID);
    }

    @Test
    void shouldFailWhenEnvironmentValueIsNotNumber() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment()
                .withProperty(InstanceNodeIdProvider.ENV_BLUECONE_NODE_ID, "abc");

        assertThrows(IllegalStateException.class,
                () -> InstanceNodeIdProvider.resolveNodeId(longId, env));
    }

    @Test
    void shouldFailWhenNodeIdOutOfRange() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        longId.setNodeId(2000L);
        MockEnvironment env = new MockEnvironment();

        assertThrows(IllegalStateException.class,
                () -> InstanceNodeIdProvider.resolveNodeId(longId, env));
    }
}

