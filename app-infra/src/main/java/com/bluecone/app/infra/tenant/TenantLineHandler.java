package com.bluecone.app.infra.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

/**
 * MyBatis-Plus 租户行级数据隔离处理器
 *
 * 功能：
 * - 实现 MyBatis-Plus 的 TenantLineHandler 接口
 * - 自动在 SQL 语句中添加租户条件（WHERE tenant_id = ?）
 * - 支持表级别的租户隔离控制
 *
 * 工作原理：
 * 1. MyBatis-Plus 在执行 SQL 前拦截
 * 2. 调用 getTenantId() 获取当前租户 ID
 * 3. 自动在 WHERE 条件中追加 tenant_id = 'xxx'
 * 4. 通过 ignoreTable() 判断是否需要跳过某些表
 *
 * 使用场景：
 * - SELECT: 自动过滤只查询当前租户的数据
 * - INSERT: 自动填充租户 ID 字段
 * - UPDATE/DELETE: 自动添加租户条件，防止误操作其他租户数据
 *
 * 未来启用方式：
 * 在 MybatisPlusConfig 中注册：
 * <pre>
 * TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandler());
 * interceptor.addInnerInterceptor(tenantInterceptor);
 * </pre>
 *
 * 注意事项：
 * - 当前未启用，仅作为架构预留
 * - 启用前需确保所有业务表都有 tenant_id 字段
 * - 系统表、配置表等公共表需要通过 ignoreTable() 排除
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
public class TenantLineHandler implements TenantLineHandler {

    /**
     * 获取租户 ID 值
     *
     * 从 TenantContext 中获取当前线程的租户 ID，并转换为 SQL 表达式
     *
     * @return 租户 ID 的 SQL 表达式（StringValue 类型）
     */
    @Override
    public Expression getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // 如果租户 ID 为空，返回空字符串（实际使用时应抛出异常或记录日志）
            return new StringValue("");
        }
        return new StringValue(tenantId);
    }

    /**
     * 获取租户字段名称
     *
     * 指定数据库表中用于存储租户 ID 的字段名
     *
     * @return 租户字段名，默认为 "tenant_id"
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 判断是否忽略租户隔离
     *
     * 某些表不需要租户隔离（如系统配置表、字典表等），可以在此方法中排除
     *
     * 示例：
     * <pre>
     * if ("sys_config".equalsIgnoreCase(tableName)) {
     *     return true;
     * }
     * if ("sys_dict".equalsIgnoreCase(tableName)) {
     *     return true;
     * }
     * return false;
     * </pre>
     *
     * @param tableName 表名
     * @return true 表示忽略租户隔离，false 表示需要租户隔离
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 当前返回 false，表示所有表都启用租户隔离
        // 未来可根据实际业务需求，排除系统表、配置表等
        return false;
    }
}
