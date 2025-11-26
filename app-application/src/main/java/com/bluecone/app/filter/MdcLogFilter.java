package com.bluecone.app.filter;

import com.bluecone.app.infra.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class MdcLogFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String TENANT_ID = "tenantId";
    private static final String USER_ID = "userId";

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID = "X-User-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 生成 traceId
            MDC.put(TRACE_ID, UUID.randomUUID().toString().replace("-", ""));

            // 从 Header 获取 requestId
            String requestId = httpRequest.getHeader(HEADER_REQUEST_ID);
            if (requestId != null && !requestId.isEmpty()) {
                MDC.put(REQUEST_ID, requestId);
            }

            // 从 TenantContext 获取 tenantId
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && !tenantId.isEmpty()) {
                MDC.put(TENANT_ID, tenantId);
            }

            // 从 Header 获取 userId（临时方案，后续从 UserContext 获取）
            String userId = httpRequest.getHeader(HEADER_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                MDC.put(USER_ID, userId);
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
