package com.bluecone.app.core.log;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ApiEvent 创建与收尾工厂，负责从请求与上下文提取基础字段。
 */
@Component
public class ApiEventFactory {

    private static final Pattern VERSION_IN_PATH = Pattern.compile("/api/v(\\d+)/");

    public ApiEvent createStartEvent(HttpServletRequest request) {
        ApiEvent event = new ApiEvent()
                .setEventType(ApiEvent.EventType.API_START)
                .setTimestamp(Instant.now())
                .setStartTimeMs(System.currentTimeMillis())
                .setPath(request != null ? request.getRequestURI() : "")
                .setMethod(request != null ? request.getMethod() : "")
                .setVersion(resolveVersion(request))
                .setTraceId(MDC.get("traceId"))
                .setRequestId(MDC.get("requestId"))
                .setTenantId(MDC.get("tenantId"))
                .setUserId(MDC.get("userId"))
                .setIp(extractClientIp(request))
                .setUserAgent(header(request, "User-Agent"))
                .setDeviceId(header(request, "X-Device-Id"))
                .setRequestBodyDigest(extractRequestDigest(request));

        return event;
    }

    public ApiEvent finalizeEvent(ApiEvent event, Object response, Exception exception) {
        long now = System.currentTimeMillis();
        Optional.ofNullable(event.getStartTimeMs())
                .ifPresent(start -> event.setLatencyMs(now - start));

        event.setTimestamp(Instant.now());

        if (exception == null) {
            event.setStatus(ApiEvent.Status.SUCCESS);
            event.setEventType(ApiEvent.EventType.API_END);
        } else {
            event.setStatus(ApiEvent.Status.FAILED);
            event.setEventType(ApiEvent.EventType.API_ERROR);
            event.setExceptionDigest(buildExceptionDigest(exception));
        }

        if (response != null) {
            event.setPayload(response);
        }

        return event;
    }

    private String resolveVersion(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String headerVersion = header(request, "X-Api-Version");
        if (StringUtils.hasText(headerVersion)) {
            return "v" + headerVersion;
        }

        String queryVersion = request.getParameter("version");
        if (StringUtils.hasText(queryVersion)) {
            return "v" + queryVersion;
        }

        Matcher matcher = VERSION_IN_PATH.matcher(request.getRequestURI() + "/");
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = header(request, "X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = header(request, "X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String extractRequestDigest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> {
            String value = v != null && v.length > 0 ? String.join(",", v) : "";
            joiner.add(k + "=" + value);
        });
        return joiner.toString();
    }

    private String buildExceptionDigest(Exception exception) {
        String simpleName = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        String digest = simpleName + (message != null ? (": " + message) : "");
        return digest.length() > 256 ? digest.substring(0, 256) : digest;
    }

    private String header(HttpServletRequest request, String name) {
        return request != null ? request.getHeader(name) : null;
    }
}
