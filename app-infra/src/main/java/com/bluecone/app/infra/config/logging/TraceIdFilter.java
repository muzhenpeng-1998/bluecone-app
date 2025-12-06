package com.bluecone.app.infra.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP TraceId 透传过滤器：
 * <p>
 * - 优先从请求头 X-Trace-Id / X-Request-Id 读取 traceId；<br>
 * - 若不存在则生成新的 UUID；<br>
 * - 写入 MDC，保证日志、Outbox 元数据、通知等链路可共享相同 traceId；<br>
 * - 请求结束后清理 MDC。
 */
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String fromTraceHeader = request.getHeader(TRACE_ID_HEADER);
        if (fromTraceHeader != null && !fromTraceHeader.isBlank()) {
            return fromTraceHeader;
        }
        String fromRequestHeader = request.getHeader(REQUEST_ID_HEADER);
        if (fromRequestHeader != null && !fromRequestHeader.isBlank()) {
            return fromRequestHeader;
        }
        return UUID.randomUUID().toString();
    }
}
