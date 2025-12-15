package com.bluecone.app.test;

import com.bluecone.app.Application;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.token.TokenProperties;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.core.tenant.TenantContext;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full application on a random port with containerized dependencies for HTTP level tests.
 */
@Testcontainers(disabledWithoutDocker = true) // 本地无 Docker 时跳过 Web 集成测试
@ActiveProfiles("test")
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.enabled=true"
})
public abstract class AbstractWebIntegrationTest {

    private static final List<String> TRUNCATE_TABLES = List.of(
            "bc_user",
            "bc_auth_session",
            "t_order",
            "bc_outbox_message"
    );

    @Container
    private static final MySqlTestContainer MYSQL = MySqlTestContainer.getInstance();

    @Container
    private static final RedisTestContainer REDIS = RedisTestContainer.getInstance();

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private TokenProperties tokenProperties;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    protected String url(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "http://localhost:" + port + path;
    }

    protected HttpHeaders tenantHeaders(Long tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-Id", tenantId != null ? tenantId.toString() : "default");
        return headers;
    }

    protected HttpHeaders authenticatedHeaders(Long userId, Long tenantId) {
        AuthTokens tokens = issueTokens(userId, tenantId);
        HttpHeaders headers = tenantHeaders(tenantId);
        headers.setBearerAuth(tokens.accessToken());
        return headers;
    }

    protected AuthTokens issueTokens(Long userId, Long tenantId) {
        String sessionId = UUID.randomUUID().toString();
        TokenUserContext ctx = TokenUserContext.builder()
                .userId(userId)
                .tenantId(tenantId)
                .sessionId(sessionId)
                .clientType("test-suite")
                .deviceId("test-device")
                .build();
        String refreshToken = tokenProvider.generateRefreshToken(ctx);
        authSessionService.createSessionOnLogin(
                sessionId,
                userId,
                tenantId,
                "test-suite",
                "test-device",
                refreshToken,
                Duration.ofDays(tokenProperties.getRefreshTokenTtlDays()));
        String accessToken = tokenProvider.generateAccessToken(ctx);
        return new AuthTokens(accessToken, refreshToken, sessionId);
    }

    /**
     * Truncate common tables for fresh state. Call in @BeforeEach when needed.
     */
    protected void cleanDatabase() {
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

    protected void flushRedis() {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @AfterEach
    void cleanupTenant() {
        TenantContext.clear();
    }

    protected record AuthTokens(String accessToken, String refreshToken, String sessionId) {
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = database() AND table_name = ?",
                Integer.class, table);
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
