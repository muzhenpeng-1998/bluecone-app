package com.bluecone.app.core.contextkit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VersionChecker 窗口与采样测试。
 */
class VersionCheckerTest {

    @Test
    void shouldRespectWindowAndSampleRate() {
        VersionChecker checker = new VersionChecker(Duration.ofSeconds(10), 1.0d);
        CacheKey key = new CacheKey("test", "1");

        boolean first = checker.shouldCheck(key);
        checker.markChecked(key);
        boolean second = checker.shouldCheck(key);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}

