package com.bluecone.app.core.exception;

import org.slf4j.MDC;

import java.time.LocalDateTime;

public class ApiErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String traceId;
    private String path;

    public ApiErrorResponse(String code, String message, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.traceId = MDC.get("traceId");
        this.path = path;
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, path);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
