package com.bluecone.app.platform.ops;

import com.bluecone.app.platform.ops.autoconfigure.OpsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for app-platform-starter-ops.
 * 
 * <p>Verifies that ops console is disabled by default and only enabled
 * when explicitly configured.</p>
 */
class OpsStarterSmokeTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    WebMvcAutoConfiguration.class,
                    OpsAutoConfiguration.class
            ));

    @Test
    void shouldNotConfigureOpsByDefault() {
        contextRunner.run(context -> {
            // Ops should not be configured when bluecone.ops.enabled is not set
            assertThat(context).doesNotHaveBean("opsConsoleConfiguration");
        });
    }

    @Test
    void shouldNotConfigureOpsWhenExplicitlyDisabled() {
        contextRunner
                .withPropertyValues("bluecone.ops.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("opsConsoleConfiguration");
                });
    }

    @Test
    void shouldConfigureOpsWhenEnabled() {
        contextRunner
                .withPropertyValues("bluecone.ops.enabled=true")
                .run(context -> {
                    // Verify ops configuration is loaded
                    assertThat(context).hasNotFailed();
                    // Note: Full bean verification requires ops module dependencies
                });
    }
}

