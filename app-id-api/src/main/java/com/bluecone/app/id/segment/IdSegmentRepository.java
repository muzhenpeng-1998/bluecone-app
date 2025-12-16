package com.bluecone.app.id.segment;

import com.bluecone.app.id.api.IdScope;

/**
 * ID 号段仓储接口（SPI），定义号段分配的数据访问契约。
 * 
 * <p>实现类需保证：
 * <ul>
 *   <li>线程安全：多线程并发调用 nextRange 必须返回不重叠的号段</li>
 *   <li>持久化：号段分配状态需持久化到数据库，避免重启后 ID 重复</li>
 *   <li>原子性：使用数据库事务 + 行锁（如 SELECT FOR UPDATE）保证原子性</li>
 * </ul>
 * 
 * <p>典型实现：基于 JDBC 的 JdbcIdSegmentRepository（位于 app-infra 模块）。
 */
public interface IdSegmentRepository {
    
    /**
     * 为指定作用域分配下一个号段。
     * 
     * <p>实现要求：
     * <ol>
     *   <li>开启数据库事务</li>
     *   <li>使用 SELECT ... FOR UPDATE 锁定 scope 对应的行</li>
     *   <li>读取当前 max_id 和 step</li>
     *   <li>计算新的 max_id = 当前 max_id + step</li>
     *   <li>更新数据库：UPDATE bc_id_segment SET max_id = 新 max_id WHERE scope = ?</li>
     *   <li>提交事务</li>
     *   <li>返回号段 [旧 max_id + 1, 新 max_id]</li>
     * </ol>
     * 
     * @param scope 作用域，对应业务表或领域
     * @param step 号段步长，即本次分配的 ID 数量（通常为 1000）
     * @return 分配的号段范围
     * @throws IllegalArgumentException 如果 scope 不存在或 step <= 0
     * @throws IllegalStateException 如果数据库操作失败
     */
    SegmentRange nextRange(IdScope scope, int step);
    
    /**
     * 初始化指定作用域的号段记录（如果不存在）。
     * 
     * <p>用于首次启动时自动创建 scope 记录，避免手动初始化。
     * 
     * @param scope 作用域
     * @param initialMaxId 初始 max_id（通常为 0）
     * @param defaultStep 默认步长（通常为 1000）
     */
    void initScopeIfAbsent(IdScope scope, long initialMaxId, int defaultStep);
}
