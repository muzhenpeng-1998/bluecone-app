package com.bluecone.app.infra.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户 Web 拦截器
 *
 * 功能：
 * - 从 HTTP 请求头中提取租户 ID
 * - 将租户 ID 存储到 TenantContext（ThreadLocal）
 * - 请求结束后清理 TenantContext，避免线程池复用导致的数据泄露
 *
 * 工作流程：
 * 1. preHandle: 请求开始时，从请求头 "X-Tenant-Id" 获取租户 ID，存入 TenantContext
 * 2. afterCompletion: 请求结束时，清理 TenantContext
 *
 * 使用场景：
 * - 前端在每个请求的 Header 中携带 X-Tenant-Id
 * - 后端自动识别租户，实现数据隔离
 * - 配合 MyBatis-Plus TenantLineInnerInterceptor 实现 SQL 级别的租户隔离
 *
 * 注意事项：
 * - 必须在 WebMvcConfig 中注册此拦截器
 * - 拦截器优先级应该较高，确保在业务逻辑执行前设置租户 ID
 * - afterCompletion 必须执行，避免 ThreadLocal 内存泄漏
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Component
public class TenantWebInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantWebInterceptor.class);

    /**
     * 租户 ID 请求头名称
     */
    private static final String TENANT_HEADER = "X-Tenant-Id";

    /**
     * 默认租户 ID
     * 当请求头中没有租户 ID 时使用
     */
    private static final String DEFAULT_TENANT_ID = "default";

    /**
     * 请求处理前执行
     *
     * 从请求头中提取租户 ID，并存储到 TenantContext
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return true 继续执行后续拦截器和处理器，false 中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取租户 ID
        String tenantId = request.getHeader(TENANT_HEADER);

        // 如果租户 ID 为空或空白，使用默认值
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT_ID;
            log.debug("租户 ID 未在请求头中找到，使用默认租户: {}", DEFAULT_TENANT_ID);
        } else {
            log.debug("从请求头中获取租户 ID: {}", tenantId);
        }

        // 将租户 ID 存储到 ThreadLocal
        TenantContext.setTenantId(tenantId);

        return true;
    }

    /**
     * 请求完成后执行（无论成功或异常）
     *
     * 清理 TenantContext，避免线程池复用导致的租户数据泄露
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常（如果有）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，避免内存泄漏
        TenantContext.clear();
        log.debug("租户上下文已清理");
    }
}
