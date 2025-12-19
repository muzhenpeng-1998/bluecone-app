package com.bluecone.app.infra.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Observability MDC Filter
 * 
 * Populates MDC (Mapped Diagnostic Context) with key observability fields:
 * - traceId: Unique identifier for the entire request trace
 * - spanId: Unique identifier for this specific span/operation
 * - requestId: HTTP request identifier (from header or generated)
 * - tenantId: Multi-tenant identifier (from header)
 * - userId: User identifier (from header)
 * - orderId: Order identifier (from header, for order-related requests)
 * 
 * These fields are automatically included in all log entries via logback-spring.xml configuration.
 */
@Slf4j
@Component
@Order(1)
public class ObservabilityMdcFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_SPAN_ID = "X-Span-Id";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ORDER_ID = "X-Order-Id";
    
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ORDER_ID = "orderId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Trace ID: from header or generate new
            String traceId = request.getHeader(HEADER_TRACE_ID);
            if (traceId == null || traceId.isEmpty()) {
                traceId = generateTraceId();
            }
            MDC.put(MDC_TRACE_ID, traceId);
            
            // Span ID: from header or generate new
            String spanId = request.getHeader(HEADER_SPAN_ID);
            if (spanId == null || spanId.isEmpty()) {
                spanId = generateSpanId();
            }
            MDC.put(MDC_SPAN_ID, spanId);
            
            // Request ID: from header or generate new
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isEmpty()) {
                requestId = generateRequestId();
            }
            MDC.put(MDC_REQUEST_ID, requestId);
            
            // Tenant ID: from header (optional)
            String tenantId = request.getHeader(HEADER_TENANT_ID);
            if (tenantId != null && !tenantId.isEmpty()) {
                MDC.put(MDC_TENANT_ID, tenantId);
            }
            
            // User ID: from header (optional)
            String userId = request.getHeader(HEADER_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                MDC.put(MDC_USER_ID, userId);
            }
            
            // Order ID: from header (optional, for order-related requests)
            String orderId = request.getHeader(HEADER_ORDER_ID);
            if (orderId != null && !orderId.isEmpty()) {
                MDC.put(MDC_ORDER_ID, orderId);
            }
            
            // Add trace ID to response header for client-side correlation
            response.setHeader(HEADER_TRACE_ID, traceId);
            response.setHeader(HEADER_REQUEST_ID, requestId);
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ORDER_ID);
        }
    }

    /**
     * Generate a trace ID (128-bit hex string, compatible with OpenTelemetry)
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Generate a span ID (64-bit hex string, compatible with OpenTelemetry)
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Generate a request ID (UUID)
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
