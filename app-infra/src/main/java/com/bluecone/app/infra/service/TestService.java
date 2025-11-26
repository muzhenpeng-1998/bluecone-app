package com.bluecone.app.infra.service;

import com.bluecone.app.infra.mapper.TestMapper;
import org.springframework.stereotype.Service;

/**
 * 测试服务类
 *
 * 用途：
 * - 提供业务层访问数据库的接口
 * - 验证 Service -> Mapper -> Database 调用链路
 * - 验证多租户拦截器在实际业务中的工作情况
 *
 * 注意：
 * - 当前仅用于测试，不包含复杂业务逻辑
 * - count() 方法会自动应用租户过滤条件
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Service
public class TestService {

    private final TestMapper testMapper;

    /**
     * 构造函数注入
     *
     * @param testMapper 测试 Mapper
     */
    public TestService(TestMapper testMapper) {
        this.testMapper = testMapper;
    }

    /**
     * 统计当前租户的记录数量
     *
     * 工作原理：
     * 1. TenantWebInterceptor 从请求头提取租户 ID，存入 TenantContext
     * 2. MyBatis-Plus 执行 SQL 时，TenantLineInnerInterceptor 自动添加 WHERE tenant_id = ?
     * 3. 返回当前租户的记录数量
     *
     * 示例 SQL：
     * SELECT COUNT(*) FROM bc_test WHERE tenant_id = 'tenantA'
     *
     * @return 当前租户的记录数量
     */
    public long count() {
        return testMapper.selectCount(null);
    }
}
