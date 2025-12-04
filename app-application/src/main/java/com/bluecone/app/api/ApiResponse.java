package com.bluecone.app.api;

import org.slf4j.MDC;

import java.time.Instant;

/**
 * 简易统一响应包装。
 * <p>占位实现：包含 code/message/data/traceId/timestamp，方便后续替换为网关统一封装。</p>
 */
public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;
    private Instant timestamp;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("OK", "success", null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
