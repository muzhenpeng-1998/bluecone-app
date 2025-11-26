package com.bluecone.app.exception;

import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(ex.getCode(), ex.getMessage(), path);

        Map<String, Object> logData = new HashMap<>();
        logData.put("type", "BusinessException");
        logData.put("code", ex.getCode());
        logData.put("message", ex.getMessage());
        logData.put("path", path);
        logData.put("traceId", MDC.get("traceId"));
        logData.put("tenantId", MDC.get("tenantId"));
        logData.put("userId", MDC.get("userId"));

        try {
            log.warn("BUSINESS-ERROR: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.warn("BUSINESS-ERROR: {}", logData);
        }

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        ApiErrorResponse response = ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage(),
                path
        );

        Map<String, Object> logData = new HashMap<>();
        logData.put("type", "SystemException");
        logData.put("code", ErrorCode.INTERNAL_ERROR.getCode());
        logData.put("message", ex.getMessage());
        logData.put("exception", ex.getClass().getName());
        logData.put("path", path);
        logData.put("traceId", MDC.get("traceId"));
        logData.put("tenantId", MDC.get("tenantId"));
        logData.put("userId", MDC.get("userId"));

        try {
            log.error("SYSTEM-ERROR: {}", objectMapper.writeValueAsString(logData), ex);
        } catch (Exception e) {
            log.error("SYSTEM-ERROR: {}", logData, ex);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
