package com.bluecone.app.infra.id;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.segment.SegmentRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * JdbcIdSegmentRepository 集成测试（使用 Testcontainers MySQL）。
 * 
 * <p>测试目标：
 * <ul>
 *   <li>数据库事务正确性：SELECT FOR UPDATE 保证并发安全</li>
 *   <li>号段分配唯一性：多线程并发申请号段不重叠</li>
 *   <li>初始化幂等性：重复初始化不报错</li>
 * </ul>
 * 
 * <p>注意：此测试需要 Docker 环境，如无 Docker 可跳过（mvn test -Dskip.it.tests=true）。
 */
@SpringBootTest(classes = JdbcIdSegmentRepositoryITConfig.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JdbcIdSegmentRepository 集成测试")
class JdbcIdSegmentRepositoryIT {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private JdbcIdSegmentRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new JdbcIdSegmentRepository(jdbcTemplate);
        
        // 创建测试表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS bc_id_segment (
                scope VARCHAR(64) NOT NULL,
                max_id BIGINT NOT NULL,
                step INT NOT NULL DEFAULT 1000,
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (scope)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // 清空测试数据
        jdbcTemplate.execute("TRUNCATE TABLE bc_id_segment");
    }
    
    @Test
    @DisplayName("初始化 scope - 幂等性")
    void testInitScopeIfAbsent() {
        // 第一次初始化
        repository.initScopeIfAbsent(IdScope.ORDER, 0L, 1000);
        
        Long maxId = jdbcTemplate.queryForObject(
            "SELECT max_id FROM bc_id_segment WHERE scope = ?",
            Long.class,
            IdScope.ORDER.scopeName()
        );
        assertThat(maxId).isEqualTo(0L);
        
        // 第二次初始化（幂等）
        repository.initScopeIfAbsent(IdScope.ORDER, 9999L, 5000);
        
        // max_id 不应变化
        maxId = jdbcTemplate.queryForObject(
            "SELECT max_id FROM bc_id_segment WHERE scope = ?",
            Long.class,
            IdScope.ORDER.scopeName()
        );
        assertThat(maxId).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("分配号段 - 单线程")
    void testNextRange_SingleThread() {
        repository.initScopeIfAbsent(IdScope.TENANT, 0L, 1000);
        
        // 第一次分配
        SegmentRange range1 = repository.nextRange(IdScope.TENANT, 1000);
        assertThat(range1.startInclusive()).isEqualTo(1L);
        assertThat(range1.endInclusive()).isEqualTo(1000L);
        assertThat(range1.size()).isEqualTo(1000L);
        
        // 第二次分配
        SegmentRange range2 = repository.nextRange(IdScope.TENANT, 1000);
        assertThat(range2.startInclusive()).isEqualTo(1001L);
        assertThat(range2.endInclusive()).isEqualTo(2000L);
        
        // 第三次分配
        SegmentRange range3 = repository.nextRange(IdScope.TENANT, 1000);
        assertThat(range3.startInclusive()).isEqualTo(2001L);
        assertThat(range3.endInclusive()).isEqualTo(3000L);
    }
    
    @Test
    @DisplayName("分配号段 - 多线程并发（无重叠）")
    void testNextRange_Concurrent() throws InterruptedException {
        repository.initScopeIfAbsent(IdScope.STORE, 0L, 100);
        
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        Set<SegmentRange> ranges = new HashSet<>();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SegmentRange range = repository.nextRange(IdScope.STORE, 100);
                    synchronized (ranges) {
                        ranges.add(range);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 断言：50 个号段，每个 100 个 ID
        assertThat(ranges).hasSize(threadCount);
        
        // 断言：所有号段不重叠
        for (SegmentRange range1 : ranges) {
            for (SegmentRange range2 : ranges) {
                if (range1 != range2) {
                    assertThat(range1.endInclusive()).isLessThan(range2.startInclusive())
                        .as("号段 %s 和 %s 不应重叠", range1, range2)
                        .or()
                        .isGreaterThan(range2.endInclusive());
                }
            }
        }
    }
    
    @Test
    @DisplayName("分配号段 - scope 不存在时抛异常")
    void testNextRange_ScopeNotExists() {
        assertThatThrownBy(() -> repository.nextRange(IdScope.PRODUCT, 1000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scope 不存在");
    }
    
    @Test
    @DisplayName("分配号段 - step 非法时抛异常")
    void testNextRange_InvalidStep() {
        repository.initScopeIfAbsent(IdScope.USER, 0L, 1000);
        
        assertThatThrownBy(() -> repository.nextRange(IdScope.USER, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("step 必须大于 0");
        
        assertThatThrownBy(() -> repository.nextRange(IdScope.USER, -100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("step 必须大于 0");
    }
}

