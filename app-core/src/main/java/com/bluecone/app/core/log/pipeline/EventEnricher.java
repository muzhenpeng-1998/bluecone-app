package com.bluecone.app.core.log.pipeline;

import com.bluecone.app.core.log.ApiEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 事件字段补全器：版本、节点、响应大小、默认值。
 */
@Component
public class EventEnricher {

    private static final Pattern VERSION_IN_PATH = Pattern.compile("/api/v(\\d+)/");
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String nodeName = resolveNodeName();

    public ApiEvent enrich(ApiEvent event) {
        if (!StringUtils.hasText(event.getVersion())) {
            event.setVersion(resolveVersionFromPath(event.getPath()));
        }
        if (!StringUtils.hasText(event.getNode())) {
            event.setNode(nodeName);
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        if (event.getStatus() == null) {
            event.setStatus(ApiEvent.Status.SUCCESS);
        }
        if (!StringUtils.hasText(event.getMethod())) {
            event.setMethod("UNKNOWN");
        }
        if (!StringUtils.hasText(event.getPath())) {
            event.setPath("/");
        }
        if (!StringUtils.hasText(event.getIp())) {
            event.setIp("0.0.0.0");
        }

        event.setResponseSize(computeResponseSize(event.getPayload()));
        return event;
    }

    private String resolveVersionFromPath(String path) {
        if (!StringUtils.hasText(path)) {
            return "v1";
        }
        Matcher matcher = VERSION_IN_PATH.matcher(path + "/");
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return "v1";
    }

    private long computeResponseSize(Object payload) {
        if (payload == null) {
            return 0L;
        }
        if (payload instanceof byte[] bytes) {
            return bytes.length;
        }
        if (payload instanceof String str) {
            return str.length();
        }
        try {
            return objectMapper.writeValueAsBytes(payload).length;
        } catch (JsonProcessingException e) {
            return 0L;
        }
    }

    private String resolveNodeName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-node";
        }
    }
}
