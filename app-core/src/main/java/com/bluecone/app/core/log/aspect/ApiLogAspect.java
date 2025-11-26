package com.bluecone.app.core.log.aspect;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.core.log.util.LogMaskUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint point, ApiLog apiLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        String httpMethod = request != null ? request.getMethod() : "";
        String path = request != null ? request.getRequestURI() : "";
        String classMethod = point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName();

        Map<String, Object> logData = buildBaseLogData(apiLog, httpMethod, path, classMethod);

        if (apiLog.logArgs()) {
            addArgs(logData, point.getArgs(), apiLog.maskSensitive());
        }

        try {
            Object result = point.proceed();
            long durationMs = System.currentTimeMillis() - startTime;

            if (apiLog.logOnSuccess()) {
                logData.put("status", "SUCCESS");
                logData.put("durationMs", durationMs);
                if (apiLog.logResult()) {
                    addResult(logData, result, apiLog.maskSensitive());
                }
                logByLevel(apiLog.successLevel(), "API-SUCCESS: {}", logData, null);
            }

            return result;

        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - startTime;

            if (apiLog.logOnError()) {
                logData.put("status", "ERROR");
                logData.put("durationMs", durationMs);

                boolean isBizError = ex instanceof BusinessException;
                if (isBizError) {
                    BusinessException bizEx = (BusinessException) ex;
                    logData.put("errorCode", bizEx.getCode());
                    logData.put("errorMessage", bizEx.getMessage());
                    logByLevel(apiLog.bizErrorLevel(), "API-BIZ-ERROR: {}", logData, null);
                } else {
                    logData.put("errorCode", "INTERNAL_ERROR");
                    logData.put("errorMessage", ex.getMessage());
                    logData.put("exception", ex.getClass().getName());
                    logByLevel(apiLog.sysErrorLevel(), "API-SYS-ERROR: {}", logData,
                              apiLog.printStackTrace() ? ex : null);
                }
            }

            throw ex;
        }
    }

    private Map<String, Object> buildBaseLogData(ApiLog apiLog, String httpMethod, String path, String classMethod) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", apiLog.value());
        data.put("httpMethod", httpMethod);
        data.put("path", path);
        data.put("classMethod", classMethod);
        data.put("traceId", MDC.get("traceId"));
        data.put("requestId", MDC.get("requestId"));
        data.put("tenantId", MDC.get("tenantId"));
        data.put("userId", MDC.get("userId"));
        return data;
    }

    private void addArgs(Map<String, Object> logData, Object[] args, boolean mask) {
        try {
            String argsJson = objectMapper.writeValueAsString(args);
            logData.put("args", mask ? LogMaskUtil.mask(argsJson) : argsJson);
        } catch (Exception e) {
            logData.put("args", "serialize-error");
        }
    }

    private void addResult(Map<String, Object> logData, Object result, boolean mask) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            logData.put("result", mask ? LogMaskUtil.mask(resultJson) : resultJson);
        } catch (Exception e) {
            logData.put("result", "serialize-error");
        }
    }

    private void logByLevel(ApiLog.LogLevel level, String message, Map<String, Object> data, Throwable ex) {
        try {
            String json = objectMapper.writeValueAsString(data);
            switch (level) {
                case DEBUG -> log.debug(message, json, ex);
                case INFO -> log.info(message, json, ex);
                case WARN -> log.warn(message, json, ex);
                case ERROR -> log.error(message, json, ex);
            }
        } catch (Exception e) {
            log.error("Log serialization failed: {}", data, e);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
