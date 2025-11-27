package com.bluecone.app.core.log.error;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 将 Throwable + HttpServletRequest 转换为结构化的 ExceptionEvent。
 */
@Component
public class ExceptionEventFactory {

    private static final int STACK_TRACE_LIMIT = 5;
    private static final String EVENT_TYPE_API_ERROR = "API_ERROR";
    private static final String EVENT_TYPE_SYSTEM_ERROR = "SYSTEM_ERROR";

    public ExceptionEvent fromException(Throwable ex, HttpServletRequest request, Integer httpStatus) {
        if (ex == null) {
            return null;
        }
        int resolvedStatus = resolveStatus(ex, httpStatus);
        String requestParams = extractRequestParams(request);

        ExceptionEvent event = ExceptionEvent.builder()
                .eventType(request != null ? EVENT_TYPE_API_ERROR : EVENT_TYPE_SYSTEM_ERROR)
                .timestamp(Instant.now())
                .traceId(MDC.get("traceId"))
                .requestId(MDC.get("requestId"))
                .tenantId(MDC.get("tenantId"))
                .userId(MDC.get("userId"))
                .path(request != null ? request.getRequestURI() : null)
                .method(request != null ? request.getMethod() : null)
                .queryString(request != null ? request.getQueryString() : null)
                .httpStatus(resolvedStatus)
                .clientIp(resolveClientIp(request))
                .userAgent(header(request, "User-Agent"))
                .requestParams(requestParams)
                .requestBodyDigest(hash(requestParams))
                .errorCode(resolveErrorCode(ex))
                .exceptionType(ex.getClass().getName())
                .message(resolveMessage(ex))
                .rootCause(resolveRootCause(ex))
                .stackTop(extractStackTop(ex))
                .severity(resolveSeverity(ex, resolvedStatus))
                .build();

        populateHandlerInfo(event, request);
        populateServiceMethod(event);
        return event;
    }

    private void populateHandlerInfo(ExceptionEvent event, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        Object handlerAttr = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handlerAttr instanceof HandlerMethod handlerMethod) {
            event.setController(handlerMethod.getBeanType().getSimpleName());
            event.setHandler(handlerMethod.getMethod().getName());
        }
    }

    private void populateServiceMethod(ExceptionEvent event) {
        if (event.getStackTop() == null || event.getStackTop().isEmpty()) {
            return;
        }
        for (String frame : event.getStackTop()) {
            if (frame.contains("Service")) {
                int paren = frame.indexOf('(');
                event.setServiceMethod(paren > 0 ? frame.substring(0, paren) : frame);
                return;
            }
        }
    }

    private List<String> extractStackTop(Throwable ex) {
        List<String> frames = new ArrayList<>();
        StackTraceElement[] elements = ex.getStackTrace();
        int limit = Math.min(STACK_TRACE_LIMIT, elements.length);
        for (int i = 0; i < limit; i++) {
            StackTraceElement el = elements[i];
            frames.add(el.getClassName() + "." + el.getMethodName() + "(" + el.getFileName() + ":" + el.getLineNumber() + ")");
        }
        return frames;
    }

    private String resolveErrorCode(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getCode();
        }
        return ErrorCode.INTERNAL_ERROR.getCode();
    }

    private String resolveMessage(Throwable ex) {
        String msg = ex.getMessage();
        return StringUtils.hasText(msg) ? msg : ex.getClass().getSimpleName();
    }

    private String resolveRootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return StringUtils.hasText(message) ? message : cause.getClass().getSimpleName();
    }

    private ExceptionSeverity resolveSeverity(Throwable ex, int httpStatus) {
        if (ex instanceof BusinessException) {
            return httpStatus >= 500 ? ExceptionSeverity.ERROR : ExceptionSeverity.WARN;
        }
        if (httpStatus >= 500) {
            return ExceptionSeverity.CRITICAL;
        }
        return ExceptionSeverity.ERROR;
    }

    private int resolveStatus(Throwable ex, Integer httpStatus) {
        if (httpStatus != null) {
            return httpStatus;
        }
        if (ex instanceof BusinessException) {
            return HttpStatus.OK.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String extractRequestParams(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) {
            return request.getQueryString();
        }
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> joiner.add(k + "=" + String.join(",", Objects.requireNonNullElseGet(v, () -> new String[]{}))));
        return joiner.toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
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

    private String header(HttpServletRequest request, String name) {
        return request != null ? request.getHeader(name) : null;
    }

    private String hash(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes());
            return "sha256:" + HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(source.hashCode());
        }
    }
}
