package com.bluecone.app.platform;

import com.bluecone.app.core.create.api.IdempotentCreateTemplate;
import com.bluecone.app.core.event.consume.api.EventHandlerTemplate;
import com.bluecone.app.core.idempotency.api.IdempotencyTemplate;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for platform starter auto-configuration wiring.
 */
class PlatformStarterSmokeTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class
            ))
            .withPropertyValues(
                    "bluecone.id.enabled=true",
                    "bluecone.id.public-id.enabled=true",
                    "bluecone.idempotency.enabled=true",
                    "bluecone.create.enabled=true",
                    "bluecone.eventing.consume.enabled=true"
            )
            .withAllowBeanDefinitionOverriding(true)
            .withBean(IdService.class, () -> null, bd -> {
                // placeholder to ensure classpath is scanned; real bean provided by auto-config
            })
            .withBean(PublicIdCodec.class, () -> null, bd -> {});

    @Test
    void platformStarterProvidesCoreBeansWhenEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        com.bluecone.app.id.autoconfigure.IdAutoConfiguration.class,
                        com.bluecone.app.id.autoconfigure.IdJacksonAutoConfiguration.class,
                        com.bluecone.app.id.autoconfigure.IdMybatisAutoConfiguration.class,
                        com.bluecone.app.core.event.consume.autoconfigure.EventConsumeAutoConfiguration.class
                ))
                .withPropertyValues(
                        "bluecone.id.enabled=true",
                        "bluecone.id.public-id.enabled=true",
                        "bluecone.id.jackson.enabled=true",
                        "bluecone.id.mybatis.enabled=true",
                        "bluecone.eventing.consume.enabled=true"
                )
                .withAllowBeanDefinitionOverriding(true)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdService.class);
                    assertThat(context).hasSingleBean(PublicIdCodec.class);
                    assertThat(context).hasBean("eventHandlerTemplate");
                });
    }

    @Test
    void idCanBeDisabledViaProperty() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        com.bluecone.app.id.autoconfigure.IdAutoConfiguration.class
                ))
                .withPropertyValues(
                        "bluecone.id.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IdService.class);
                });
    }
}

