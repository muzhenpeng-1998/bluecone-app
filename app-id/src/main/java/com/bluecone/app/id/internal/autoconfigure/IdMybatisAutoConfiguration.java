package com.bluecone.app.id.internal.autoconfigure;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.internal.mybatis.Ulid128BinaryTypeHandler;
import com.bluecone.app.id.internal.mybatis.Ulid128Char26TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatis 相关自动配置：为 Ulid128 注册 TypeHandler。
 */
@AutoConfiguration(after = IdAutoConfiguration.class)
@ConditionalOnClass(name = {
        "org.apache.ibatis.session.Configuration",
        "org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer"
})
@ConditionalOnProperty(prefix = "bluecone.id.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdMybatisAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IdMybatisAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public Ulid128BinaryTypeHandler ulid128BinaryTypeHandler() {
        log.info("装配 Ulid128BinaryTypeHandler");
        return new Ulid128BinaryTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public Ulid128Char26TypeHandler ulid128Char26TypeHandler() {
        log.info("装配 Ulid128Char26TypeHandler");
        return new Ulid128Char26TypeHandler();
    }

    @Bean
    public ConfigurationCustomizer blueconeUlidTypeHandlerCustomizer(Ulid128BinaryTypeHandler binary,
                                                                     Ulid128Char26TypeHandler char26) {
        return new ConfigurationCustomizer() {
            @Override
            public void customize(Configuration configuration) {
                log.info("注册 Ulid128 TypeHandler 到 MyBatis");
                TypeHandlerRegistry r = configuration.getTypeHandlerRegistry();
                // 默认将 Ulid128 映射为二进制处理器
                r.register(Ulid128.class, binary);
                // 对于显式声明的 JDBC 类型，进行精细注册
                r.register(Ulid128.class, JdbcType.BINARY, binary);
                r.register(Ulid128.class, JdbcType.VARBINARY, binary);
                // CHAR/VARCHAR 显式指定时使用字符串形式
                r.register(Ulid128.class, JdbcType.CHAR, char26);
                r.register(Ulid128.class, JdbcType.VARCHAR, char26);
                log.info("Ulid128 TypeHandler 注册完成");
            }
        };
    }
}
