package com.bluecone.app.gateway.middleware;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.ApiEventFactory;
import com.bluecone.app.core.log.pipeline.EventPipeline;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes trace metadata into MDC and emits structured API events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingMiddleware implements ApiMiddleware {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_TENANT = "tenantId";
    private static final String MDC_USER = "userId";

    private final ApiEventFactory apiEventFactory;
    private final EventPipeline eventPipeline;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        String originalTraceId = MDC.get(MDC_TRACE_ID);
        try {
            String traceId = resolveTraceId(ctx);
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put("requestId", traceId);
            if (ctx.getTenantId() != null) {
                MDC.put(MDC_TENANT, ctx.getTenantId());
            }
            if (ctx.getUserId() != null) {
                MDC.put(MDC_USER, String.valueOf(ctx.getUserId()));
            }

            ApiEvent startEvent = apiEventFactory.createStartEvent(ctx.getRequest());
            chain.next(ctx);
            ApiEvent endEvent = apiEventFactory.finalizeEvent(startEvent, ctx.getResponse(), ctx.getError());
            eventPipeline.process(endEvent);
        } catch (Exception ex) {
            ctx.setError(ex);
            log.error("Gateway logging middleware caught exception", ex);
            throw ex;
        } finally {
            if (originalTraceId != null) {
                MDC.put(MDC_TRACE_ID, originalTraceId);
            } else {
                MDC.remove(MDC_TRACE_ID);
            }
            MDC.remove("requestId");
            MDC.remove(MDC_TENANT);
            MDC.remove(MDC_USER);
        }
    }

    private String resolveTraceId(ApiContext ctx) {
        if (ctx.getTraceId() != null) {
            return ctx.getTraceId();
        }
        String incoming = ctx.getRequest() != null
                ? ctx.getRequest().getHeader("X-Trace-Id")
                : null;
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
