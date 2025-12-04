package com.bluecone.app.core.error;

/**
 * 系统级通用错误码。
 * <p>与具体业务无关，例如参数错误、系统异常等。</p>
 */
public enum CommonErrorCode implements ErrorCode {

    SYSTEM_ERROR("SYS-500-000", "系统异常，请稍后重试"),
    BAD_REQUEST("SYS-400-000", "请求参数错误"),
    UNAUTHORIZED("SYS-401-000", "未登录或登录已失效"),
    FORBIDDEN("SYS-403-000", "没有权限执行该操作");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
