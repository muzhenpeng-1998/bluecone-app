package com.bluecone.app.infra.id;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.segment.SegmentRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基于 JDBC 的 ID 号段仓储实现。
 * 
 * <p>核心特性：
 * <ul>
 *   <li>事务保证：使用 Spring 声明式事务 + SELECT FOR UPDATE 保证原子性</li>
 *   <li>线程安全：数据库行锁保证多实例并发安全</li>
 *   <li>高性能：单次分配大批量 ID（如 1000 个），减少数据库访问</li>
 * </ul>
 * 
 * <p>依赖表结构：
 * <pre>
 * CREATE TABLE bc_id_segment (
 *   scope VARCHAR(64) NOT NULL,
 *   max_id BIGINT NOT NULL,
 *   step INT NOT NULL DEFAULT 1000,
 *   updated_at DATETIME NOT NULL,
 *   PRIMARY KEY (scope)
 * );
 * </pre>
 */
public class JdbcIdSegmentRepository implements IdSegmentRepository {
    
    private static final Logger log = LoggerFactory.getLogger(JdbcIdSegmentRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 构造函数。
     * 
     * @param jdbcTemplate Spring JdbcTemplate
     */
    public JdbcIdSegmentRepository(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException("jdbcTemplate 不能为 null");
        }
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 为指定作用域分配下一个号段。
     * 
     * <p>实现步骤：
     * <ol>
     *   <li>开启事务（由 @Transactional 保证）</li>
     *   <li>SELECT ... FOR UPDATE 锁定 scope 行</li>
     *   <li>读取当前 max_id</li>
     *   <li>计算新的 max_id = 当前 max_id + step</li>
     *   <li>更新数据库</li>
     *   <li>提交事务</li>
     *   <li>返回号段 [旧 max_id + 1, 新 max_id]</li>
     * </ol>
     * 
     * @param scope 作用域
     * @param step 号段步长
     * @return 分配的号段范围
     * @throws IllegalArgumentException 如果 scope 不存在或 step <= 0
     * @throws IllegalStateException 如果数据库操作失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SegmentRange nextRange(IdScope scope, int step) {
        if (scope == null) {
            throw new IllegalArgumentException("scope 不能为 null");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step 必须大于 0，当前值: " + step);
        }
        
        String scopeName = scope.scopeName();
        
        try {
            // 1. 使用 FOR UPDATE 锁定行，保证并发安全
            String selectSql = "SELECT max_id FROM bc_id_segment WHERE scope = ? FOR UPDATE";
            Long currentMaxId = jdbcTemplate.queryForObject(selectSql, Long.class, scopeName);
            
            if (currentMaxId == null) {
                throw new IllegalStateException("查询 max_id 返回 null，scope: " + scopeName);
            }
            
            // 2. 计算新的 max_id
            long newMaxId = currentMaxId + step;
            
            // 3. 更新数据库
            String updateSql = "UPDATE bc_id_segment SET max_id = ?, updated_at = NOW() WHERE scope = ?";
            int updated = jdbcTemplate.update(updateSql, newMaxId, scopeName);
            
            if (updated != 1) {
                throw new IllegalStateException(
                    String.format("更新 bc_id_segment 失败，scope: %s, updated: %d", scopeName, updated)
                );
            }
            
            // 4. 返回号段 [currentMaxId + 1, newMaxId]
            long startInclusive = currentMaxId + 1;
            long endInclusive = newMaxId;
            
            log.debug("成功分配号段: scope={}, range=[{}, {}], size={}", 
                     scopeName, startInclusive, endInclusive, step);
            
            return new SegmentRange(startInclusive, endInclusive);
            
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException(
                String.format("scope 不存在: %s，请先执行 initScopeIfAbsent 或手动插入记录", scopeName), e
            );
        } catch (Exception e) {
            log.error("分配号段失败: scope={}, step={}", scopeName, step, e);
            throw new IllegalStateException("分配号段失败: scope=" + scopeName, e);
        }
    }
    
    /**
     * 初始化指定作用域的号段记录（如果不存在）。
     * 
     * <p>使用 INSERT ... ON DUPLICATE KEY UPDATE 实现幂等性。
     * 
     * @param scope 作用域
     * @param initialMaxId 初始 max_id（通常为 0）
     * @param defaultStep 默认步长（通常为 1000）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initScopeIfAbsent(IdScope scope, long initialMaxId, int defaultStep) {
        if (scope == null) {
            throw new IllegalArgumentException("scope 不能为 null");
        }
        if (defaultStep <= 0) {
            throw new IllegalArgumentException("defaultStep 必须大于 0，当前值: " + defaultStep);
        }
        
        String scopeName = scope.scopeName();
        
        try {
            // 使用 INSERT ... ON DUPLICATE KEY UPDATE 实现幂等
            String sql = "INSERT INTO bc_id_segment (scope, max_id, step, updated_at) " +
                         "VALUES (?, ?, ?, NOW()) " +
                         "ON DUPLICATE KEY UPDATE updated_at = updated_at"; // 已存在则不更新
            
            int inserted = jdbcTemplate.update(sql, scopeName, initialMaxId, defaultStep);
            
            if (inserted > 0) {
                log.info("成功初始化 scope: {}, initialMaxId={}, defaultStep={}", 
                        scopeName, initialMaxId, defaultStep);
            } else {
                log.debug("scope 已存在，跳过初始化: {}", scopeName);
            }
            
        } catch (Exception e) {
            log.error("初始化 scope 失败: {}", scopeName, e);
            throw new IllegalStateException("初始化 scope 失败: " + scopeName, e);
        }
    }
}

