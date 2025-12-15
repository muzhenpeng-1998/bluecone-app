package com.bluecone.app.infra.test;

import com.bluecone.app.core.tenant.TenantContext;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for infrastructure integration tests. Spins up containerized MySQL/Redis once and
 * wires Spring Boot against them. Provides utility helpers to keep state isolated.
 */
@Testcontainers(disabledWithoutDocker = true) // 本地无 Docker 环境时自动跳过集成测试
@ActiveProfiles("test")
@SpringBootTest(classes = InfraTestApplication.class)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class AbstractIntegrationTest {

    private static final List<String> TRUNCATE_TABLES = List.of(
            "bc_user",
            "bc_auth_session",
            "t_order",
            "bc_outbox_message",
            "bc_config_property",
            "bc_scheduler_job",
            "bc_scheduler_job_execution_log",
            "bc_payment_order",
            "bc_payment_refund",
            "bc_payment_notify_log",
            "bc_payment_reconcile_record",
            "bc_integration_subscription",
            "bc_integration_delivery"
    );

    @Container
    private static final MySqlTestContainer MYSQL = MySqlTestContainer.getInstance();

    @Container
    private static final RedisTestContainer REDIS = RedisTestContainer.getInstance();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    private static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> false);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());
    }

    protected JdbcTemplate jdbcTemplate() {
        if (jdbcTemplate == null && dataSource != null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        return jdbcTemplate;
    }

    /**
     * Truncate common tables to keep each test hermetic.
     */
    protected void cleanDatabase() {
        JdbcTemplate template = jdbcTemplate();
        if (template == null) {
            return;
        }
        template.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String table : TRUNCATE_TABLES) {
                if (tableExists(template, table)) {
                    template.execute("TRUNCATE TABLE " + table);
                }
            }
        } finally {
            template.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    /**
     * Flush Redis DB to guarantee isolation between tests.
     */
    protected void flushRedis() {
        if (stringRedisTemplate == null) {
            return;
        }
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @AfterEach
    void resetTenantContext() {
        TenantContext.clear();
    }

    private boolean tableExists(JdbcTemplate template, String tableName) {
        Integer count = template.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    private static final class MySqlTestContainer extends MySQLContainer<MySqlTestContainer> {
        private static final DockerImageName IMAGE = DockerImageName.parse("mysql:8.3.0");
        private static final MySqlTestContainer INSTANCE = new MySqlTestContainer();

        private MySqlTestContainer() {
            super(IMAGE);
            withDatabaseName("bluecone");
            withUsername("bluecone");
            withPassword("bluecone");
            withReuse(true);
        }

        private static MySqlTestContainer getInstance() {
            return INSTANCE;
        }
    }

    private static final class RedisTestContainer extends GenericContainer<RedisTestContainer> {
        private static final DockerImageName IMAGE = DockerImageName.parse("redis:7.2.4-alpine");
        private static final RedisTestContainer INSTANCE = new RedisTestContainer();

        private RedisTestContainer() {
            super(IMAGE);
            addExposedPort(6379);
            withReuse(true);
        }

        private static RedisTestContainer getInstance() {
            return INSTANCE;
        }
    }
}
