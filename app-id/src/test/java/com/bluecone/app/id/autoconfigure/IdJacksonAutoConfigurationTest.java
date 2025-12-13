package com.bluecone.app.id.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.fasterxml.jackson.databind.Module;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Jackson 自动装配测试。
 */
class IdJacksonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(
                    AutoConfigurations.of(IdAutoConfiguration.class, IdJacksonAutoConfiguration.class));

    @Test
    void jacksonModuleShouldBeAutoConfiguredByDefault() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(PublicIdCodec.class);
            assertThat(ctx).hasSingleBean(Module.class);
        });
    }

    @Test
    void disabledPropertyShouldPreventModuleRegistration() {
        contextRunner
                .withPropertyValues("bluecone.id.jackson.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(Module.class);
                });
    }
}

