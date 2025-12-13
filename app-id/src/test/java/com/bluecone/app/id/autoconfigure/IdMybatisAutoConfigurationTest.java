package com.bluecone.app.id.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler;
import com.bluecone.app.id.mybatis.Ulid128Char26TypeHandler;

/**
 * MyBatis 相关自动装配测试。
 */
class IdMybatisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(IdMybatisAutoConfiguration.class));

    @Test
    void defaultEnabledShouldRegisterTypeHandlers() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Ulid128BinaryTypeHandler.class);
            assertThat(ctx).hasSingleBean(Ulid128Char26TypeHandler.class);
            assertThat(ctx).hasSingleBean(ConfigurationCustomizer.class);

            ConfigurationCustomizer customizer = ctx.getBean(ConfigurationCustomizer.class);
            Configuration configuration = new Configuration();
            customizer.customize(configuration);

            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            assertThat(registry.hasTypeHandler(Ulid128.class, JdbcType.BINARY)).isTrue();
            assertThat(registry.hasTypeHandler(Ulid128.class, JdbcType.VARBINARY)).isTrue();
        });
    }

    @Test
    void disabledPropertyShouldNotRegisterBeans() {
        contextRunner
                .withPropertyValues("bluecone.id.mybatis.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(Ulid128BinaryTypeHandler.class);
                    assertThat(ctx).doesNotHaveBean(Ulid128Char26TypeHandler.class);
                    assertThat(ctx).doesNotHaveBean(ConfigurationCustomizer.class);
                });
    }
}
