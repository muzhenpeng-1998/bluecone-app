package com.bluecone.app.product.test;

import com.bluecone.app.core.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;

/**
 * 商品模块集成测试基类（Prompt 10）。
 * <p>
 * 使用 Testcontainers 启动 MySQL 8.3 和 Redis 7.2 容器。
 * <p>
 * 特性：
 * <ul>
 *   <li>容器复用：withReuse(true)，加速测试执行</li>
 *   <li>自动清理：每个测试后清理数据库和 Redis</li>
 *   <li>Flyway 迁移：自动执行 DDL 脚本</li>
 *   <li>租户上下文：自动清理 TenantContext</li>
 * </ul>
 * <p>
 * 使用方法：
 * <pre>
 * &#64;SpringBootTest
 * class MyTest extends AbstractProductIntegrationTest {
 *     &#64;Test
 *     void test() {
 *         // 测试代码
 *     }
 * }
 * </pre>
 *
 * @author System
 * @since 2025-12-21
 */
@Testcontainers(disabledWithoutDocker = true) // 本地无 Docker 环境时自动跳过集成测试
@ActiveProfiles("test")
@SpringBootTest(classes = ProductTestApplication.class)
public abstract class AbstractProductIntegrationTest {

    /**
     * 需要清理的表列表（按依赖关系排序）。
     */
    private static final List<String> TRUNCATE_TABLES = List.of(
            // 商品相关子表（先删除子表）
            "bc_product_sku",
            "bc_product_spec_group",
            "bc_product_spec_option",
            "bc_product_attr_group_rel",
            "bc_product_attr_rel",
            "bc_product_addon_group_rel",
            "bc_product_addon_rel",
            "bc_product_category_rel",
            "bc_product_store_config",
            "bc_store_menu_snapshot",
            // 商品主表
            "bc_product",
            // 分类
            "bc_product_category",
            // 属性素材库
            "bc_product_attr_group",
            "bc_product_attr_option",
            // 小料素材库
            "bc_addon_group",
            "bc_addon_item"
    );

    @Container
    private static final MySqlTestContainer MYSQL = MySqlTestContainer.getInstance();

    @Container
    private static final RedisTestContainer REDIS = RedisTestContainer.getInstance();

    @Autowired
    protected DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    private static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());
    }

    @BeforeEach
    void initJdbcTemplate() {
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    /**
     * 清理数据库（每个测试后执行）。
     */
    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String table : TRUNCATE_TABLES) {
                if (tableExists(table)) {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table);
                }
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    /**
     * 清理租户上下文（每个测试后执行）。
     */
    @AfterEach
    void resetTenantContext() {
        TenantContext.clear();
    }

    /**
     * 检查表是否存在。
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * MySQL 测试容器（单例，支持复用）。
     */
    private static final class MySqlTestContainer extends MySQLContainer<MySqlTestContainer> {
        private static final DockerImageName IMAGE = DockerImageName.parse("mysql:8.3.0");
        private static final MySqlTestContainer INSTANCE = new MySqlTestContainer();

        private MySqlTestContainer() {
            super(IMAGE);
            withDatabaseName("bluecone_test");
            withUsername("bluecone");
            withPassword("bluecone");
            withReuse(true); // 容器复用，加速测试
        }

        private static MySqlTestContainer getInstance() {
            return INSTANCE;
        }
    }

    /**
     * Redis 测试容器（单例，支持复用）。
     */
    private static final class RedisTestContainer extends GenericContainer<RedisTestContainer> {
        private static final DockerImageName IMAGE = DockerImageName.parse("redis:7.2.4-alpine");
        private static final RedisTestContainer INSTANCE = new RedisTestContainer();

        private RedisTestContainer() {
            super(IMAGE);
            addExposedPort(6379);
            withReuse(true); // 容器复用，加速测试
        }

        private static RedisTestContainer getInstance() {
            return INSTANCE;
        }
    }
}

