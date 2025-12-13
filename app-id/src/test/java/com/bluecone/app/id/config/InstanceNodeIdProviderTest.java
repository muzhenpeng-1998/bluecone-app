package com.bluecone.app.id.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

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

    @Test
    void shouldFailWhenNoNodeIdConfigured() {
        BlueconeIdProperties.LongId longId = new BlueconeIdProperties.LongId();
        MockEnvironment env = new MockEnvironment();

        assertThrows(IllegalStateException.class,
                () -> InstanceNodeIdProvider.resolveNodeId(longId, env));
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

