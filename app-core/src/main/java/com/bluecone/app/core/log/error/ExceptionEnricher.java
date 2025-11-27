package com.bluecone.app.core.log.error;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * 异常事件富化器：补齐缺失字段，做轻量推断。
 */
@Component
public class ExceptionEnricher {

    public ExceptionEvent enrich(ExceptionEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        if (!StringUtils.hasText(event.getEventType())) {
            event.setEventType("SYSTEM_ERROR");
        }
        if (!StringUtils.hasText(event.getMethod())) {
            event.setMethod("UNKNOWN");
        }
        if (!StringUtils.hasText(event.getPath())) {
            event.setPath("/");
        }
        if (!StringUtils.hasText(event.getClientIp())) {
            event.setClientIp("0.0.0.0");
        }
        inferFromStack(event);
        normalizeEmpty(event);
        return event;
    }

    private void inferFromStack(ExceptionEvent event) {
        List<String> stack = event.getStackTop();
        if (CollectionUtils.isEmpty(stack)) {
            return;
        }
        String first = stack.get(0);
        if (!StringUtils.hasText(event.getHandler())) {
            event.setHandler(extractMethod(first));
        }
        if (!StringUtils.hasText(event.getController())) {
            event.setController(extractClass(first));
        }
        if (!StringUtils.hasText(event.getServiceMethod())) {
            for (String frame : stack) {
                if (frame.contains("Service")) {
                    event.setServiceMethod(extractMethodWithClass(frame));
                    break;
                }
            }
        }
    }

    private void normalizeEmpty(ExceptionEvent event) {
        if (!StringUtils.hasText(event.getErrorCode())) {
            event.setErrorCode("UNKNOWN_ERROR");
        }
        if (!StringUtils.hasText(event.getExceptionType())) {
            event.setExceptionType("java.lang.Exception");
        }
    }

    private String extractMethod(String frame) {
        int paren = frame.indexOf('(');
        String head = paren > 0 ? frame.substring(0, paren) : frame;
        int dot = head.lastIndexOf('.');
        return dot >= 0 ? head.substring(dot + 1) : head;
    }

    private String extractMethodWithClass(String frame) {
        int paren = frame.indexOf('(');
        return paren > 0 ? frame.substring(0, paren) : frame;
    }

    private String extractClass(String frame) {
        int paren = frame.indexOf('(');
        String head = paren > 0 ? frame.substring(0, paren) : frame;
        String[] parts = head.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2];
        }
        return head;
    }
}
