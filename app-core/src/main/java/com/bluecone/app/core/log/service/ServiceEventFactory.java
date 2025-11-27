package com.bluecone.app.core.log.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.util.LogMaskUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 构建 Service 事件/异常事件，统一脱敏、摘要与基础字段。
 */
@Component
public class ServiceEventFactory {

    private static final int MAX_DIGEST_LENGTH = 1024;

    public ServiceEvent successEvent(ProceedingJoinPoint pjp, Object result, long elapsedMs) {
        ServiceEvent event = baseEvent(pjp, elapsedMs);
        event.setEventType(ApiEvent.EventType.API_END);
        event.setStatus(ApiEvent.Status.SUCCESS);
        event.setOutcome("SUCCESS");
        event.setResultDigest(maskAndLimit(toDigest(result)));
        return event;
    }

    public ServiceExceptionEvent exceptionEvent(ProceedingJoinPoint pjp, Throwable throwable, long elapsedMs) {
        ServiceExceptionEvent event = new ServiceExceptionEvent();
        ServiceEvent base = baseEvent(pjp, elapsedMs);
        copyBase(base, event);
        event.setEventType(ApiEvent.EventType.API_ERROR);
        event.setStatus(ApiEvent.Status.FAILED);
        event.setOutcome("FAILED");
        event.setExceptionType(throwable.getClass().getName());
        event.setExceptionMessage(maskAndLimit(throwable.getMessage()));
        event.setRootCause(maskAndLimit(resolveRootCause(throwable)));
        event.setErrorCode(resolveErrorCode(throwable));
        event.setResultDigest(null);
        return event;
    }

    private ServiceEvent baseEvent(ProceedingJoinPoint pjp, long elapsedMs) {
        Signature signature = pjp.getSignature();
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();

        ServiceEvent event = new ServiceEvent();
        event.setEventName("SERVICE_CALL");
        event.setPath(className + "." + methodName);
        event.setMethod(resolveMethodName(signature));
        event.setServiceClass(className);
        event.setServiceMethod(methodName);
        event.setArgsDigest(maskAndLimit(toDigest(pjp.getArgs())));
        event.setElapsedMs(elapsedMs);
        event.setLatencyMs(elapsedMs);
        event.setTraceId(MDC.get("traceId"));
        event.setRequestId(MDC.get("requestId"));
        event.setTenantId(MDC.get("tenantId"));
        event.setUserId(MDC.get("userId"));
        return event;
    }

    private void copyBase(ServiceEvent source, ServiceExceptionEvent target) {
        target.setEventName(source.getEventName());
        target.setPath(source.getPath());
        target.setMethod(source.getMethod());
        target.setServiceClass(source.getServiceClass());
        target.setServiceMethod(source.getServiceMethod());
        target.setArgsDigest(source.getArgsDigest());
        target.setElapsedMs(source.getElapsedMs());
        target.setLatencyMs(source.getLatencyMs());
        target.setTraceId(source.getTraceId());
        target.setRequestId(source.getRequestId());
        target.setTenantId(source.getTenantId());
        target.setUserId(source.getUserId());
    }

    private String resolveMethodName(Signature signature) {
        if (signature instanceof MethodSignature methodSignature) {
            Method method = methodSignature.getMethod();
            return method.getName();
        }
        return signature.getName();
    }

    private String toDigest(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass().isArray()) {
            return maskAndLimit(arrayToString(obj));
        }
        return obj instanceof CharSequence ? obj.toString() : obj.getClass().getSimpleName() + ":" + safeToString(obj);
    }

    private String safeToString(Object obj) {
        try {
            return Objects.toString(obj);
        } catch (Exception e) {
            return obj.getClass().getSimpleName();
        }
    }

    private String resolveRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return StringUtils.hasText(msg) ? msg : cause.getClass().getSimpleName();
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable instanceof BusinessException businessException) {
            return businessException.getCode();
        }
        return ErrorCode.INTERNAL_ERROR.getCode();
    }

    private String maskAndLimit(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String masked = LogMaskUtil.mask(source);
        if (masked.length() > MAX_DIGEST_LENGTH) {
            return masked.substring(0, MAX_DIGEST_LENGTH) + "...[truncated]";
        }
        return masked;
    }

    private String toDigest(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (Object arg : args) {
            joiner.add(maskAndLimit(digestValue(arg)));
        }
        String combined = joiner.toString();
        if (combined.length() <= MAX_DIGEST_LENGTH) {
            return combined;
        }
        return hash(combined);
    }

    private String digestValue(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof CharSequence) {
            return arg.toString();
        }
        if (arg.getClass().isArray()) {
            return arrayToString(arg);
        }
        return arg.getClass().getSimpleName() + ":" + safeToString(arg);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String arrayToString(Object array) {
        if (array instanceof Object[] objArray) {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (Object o : objArray) {
                joiner.add(maskAndLimit(Objects.toString(o)));
            }
            return joiner.toString();
        }
        if (array instanceof int[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof long[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof double[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof float[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof boolean[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof byte[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof short[] a) {
            return java.util.Arrays.toString(a);
        }
        if (array instanceof char[] a) {
            return java.util.Arrays.toString(a);
        }
        return "[array]";
    }
}
