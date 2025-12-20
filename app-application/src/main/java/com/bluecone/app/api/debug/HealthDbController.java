package com.bluecone.app.controller;

import com.bluecone.app.infra.service.TestService;
import com.bluecone.app.core.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库健康检查控制器
 *
 * 用途：
 * - 验证数据库连接是否正常
 * - 验证 MyBatis-Plus 是否正常工作
 * - 验证多租户拦截器是否正确应用
 * - 验证完整的请求链路：HTTP -> Interceptor -> Service -> Mapper -> Database
 *
 * 测试方法：
 * 1. 不带租户头：curl http://localhost:8080/health/db
 *    → 使用默认租户 "default"
 *
 * 2. 带租户头：curl -H "X-Tenant-Id: tenantA" http://localhost:8080/health/db
 *    → 使用指定租户 "tenantA"
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/health")
public class HealthDbController {

    private final TestService testService;

    /**
     * 构造函数注入
     *
     * @param testService 测试服务
     */
    public HealthDbController(TestService testService) {
        this.testService = testService;
    }

    /**
     * 数据库健康检查接口
     *
     * 功能：
     * 1. 从 TenantContext 获取当前租户 ID
     * 2. 调用 TestService.count() 查询当前租户的记录数
     * 3. 返回租户 ID 和记录数
     *
     * 工作流程：
     * 1. TenantWebInterceptor 拦截请求，从 Header 提取 X-Tenant-Id
     * 2. 租户 ID 存入 TenantContext（ThreadLocal）
     * 3. Controller 调用 Service
     * 4. Service 调用 Mapper
     * 5. MyBatis-Plus 执行 SQL 时，TenantLineInnerInterceptor 自动添加 WHERE tenant_id = ?
     * 6. 返回结果
     *
     * @return 包含租户 ID 和记录数的 JSON 对象
     */
    @GetMapping("/db")
    public Map<String, Object> checkDatabase() {
        // 从 TenantContext 获取当前租户 ID
        String tenantId = TenantContext.getTenantId();

        // 查询当前租户的记录数（会自动应用租户过滤）
        long count = testService.count();

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("count", count);
        result.put("status", "ok");
        result.put("message", "Database connection is healthy");

        return result;
    }
}
