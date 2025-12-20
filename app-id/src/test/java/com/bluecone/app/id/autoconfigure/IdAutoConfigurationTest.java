package com.bluecone.app.id.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.publicid.api.PublicIdCodec;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * IdAutoConfiguration 自动装配行为测试。
 */
class IdAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(IdAutoConfiguration.class));

    /**
     * 默认开启：应装配 UlidIdGenerator 与 IdService。
     */
    @Test
    void defaultEnabledShouldCreateBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(UlidIdGenerator.class);
            assertThat(ctx).hasSingleBean(IdService.class);
        });
    }

    /**
     * 配置禁用：bluecone.id.enabled=false 时不应装配 Bean。
     */
    @Test
    void disabledPropertyShouldPreventBeans() {
        contextRunner
                .withPropertyValues("bluecone.id.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(UlidIdGenerator.class);
                    assertThat(ctx).doesNotHaveBean(IdService.class);
                });
    }

    /**
     * 业务侧自定义 IdService Bean 时，应覆盖自动装配的 IdService。
     */
    @Test
    void customIdServiceShouldOverrideAutoConfigured() {
        IdService customIdService = new IdService() {
            @Override
            public com.bluecone.app.id.core.Ulid128 nextUlid() {
                return new com.bluecone.app.id.core.Ulid128(0L, 0L);
            }

            @Override
            public String nextUlidString() {
                return "CUSTOM";
            }

            @Override
            public byte[] nextUlidBytes() {
                return new byte[16];
            }
        };

        contextRunner
                .withBean(IdService.class, () -> customIdService)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(IdService.class);
                    assertThat(ctx.getBean(IdService.class)).isSameAs(customIdService);
                    // UlidIdGenerator 仍应由自动装配提供
                    assertThat(ctx).hasSingleBean(UlidIdGenerator.class);
                });
    }

    /**
     * 存在 MeterRegistry 时，生成一次 ID 后应产出指标。
     */
    @Test
    void metricsShouldBeRecordedWhenMeterRegistryPresent() {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(Clock.class, Clock::systemUTC)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(MeterRegistry.class);
                    IdService idService = ctx.getBean(IdService.class);

                    // 触发一次生成
                    idService.nextUlidString();

                    MeterRegistry registry = ctx.getBean(MeterRegistry.class);
                    assertThat(registry.find("bluecone.id.ulid.generated.total").counter())
                            .describedAs("应存在生成次数指标")
                            .isNotNull();
                    assertThat(registry.find("bluecone.id.ulid.generated.total").counter().count())
                            .isGreaterThanOrEqualTo(1.0);
                });
    }

    /**
     * PublicIdCodec 默认应被自动装配。
     */
    @Test
    void publicIdCodecShouldBeAutoConfiguredByDefault() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(PublicIdCodec.class);
        });
    }
}
