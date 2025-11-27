package com.bluecone.app.core.log.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

/**
 * Service 事件富化：补齐 trace 信息、节点与默认值，并保证脱敏后的字段长度可控。
 */
@Component
public class ServiceEventEnricher {

    private final String node = resolveNode();

    public ServiceEvent enrich(ServiceEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        if (!StringUtils.hasText(event.getTraceId())) {
            event.setTraceId(MDC.get("traceId"));
        }
        if (!StringUtils.hasText(event.getRequestId())) {
            event.setRequestId(MDC.get("requestId"));
        }
        if (!StringUtils.hasText(event.getTenantId())) {
            event.setTenantId(MDC.get("tenantId"));
        }
        if (!StringUtils.hasText(event.getUserId())) {
            event.setUserId(MDC.get("userId"));
        }
        if (!StringUtils.hasText(event.getNode())) {
            event.setNode(node);
        }
        if (event.getElapsedMs() == null && event.getLatencyMs() != null) {
            event.setElapsedMs(event.getLatencyMs());
        }
        if (!StringUtils.hasText(event.getEventName())) {
            event.setEventName("SERVICE_CALL");
        }
        if (!StringUtils.hasText(event.getPath())) {
            event.setPath(event.getServiceClass() + "." + event.getServiceMethod());
        }
        if (!StringUtils.hasText(event.getMethod())) {
            event.setMethod(event.getServiceMethod());
        }
        return event;
    }

    private String resolveNode() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-node";
        }
    }
}
