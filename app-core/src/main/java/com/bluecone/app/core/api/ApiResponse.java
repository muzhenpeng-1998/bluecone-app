package com.bluecone.app.core.api;

import org.slf4j.MDC;

import java.time.Instant;

/**
 * 统一 API 响应包装。
 * <p>用于所有对外 API 接口的响应封装，包含标准字段：code/message/data/traceId/timestamp。</p>
 * <p>提供静态工厂方法简化创建：ok(data)、ok()、fail(code, message)。</p>
 *
 * @param <T> 响应数据类型
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

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "success", null);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // Backward compatibility aliases
    
    /**
     * 成功响应（带数据）- 向后兼容别名
     * @deprecated 使用 {@link #ok(Object)} 替代
     */
    @Deprecated
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    /**
     * 成功响应（无数据）- 向后兼容别名
     * @deprecated 使用 {@link #ok()} 替代
     */
    @Deprecated
    public static ApiResponse<Void> success() {
        return ok();
    }

    // Getters

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
