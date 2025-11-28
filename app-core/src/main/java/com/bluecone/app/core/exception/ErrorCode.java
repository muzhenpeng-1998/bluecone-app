package com.bluecone.app.core.exception;

public enum ErrorCode {

    INVALID_PARAM("INVALID_PARAM", "参数非法"),
    PARAM_MISSING("PARAM_MISSING", "缺少必需参数"),
    PARAM_INVALID("PARAM_INVALID", "参数值非法"),
    NOT_FOUND("NOT_FOUND", "数据不存在"),
    PERMISSION_DENIED("PERMISSION_DENIED", "无权限"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token 过期"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "用户名或密码错误"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "凭证无效或已过期"),
    AUTH_SESSION_INVALID("AUTH_SESSION_INVALID", "会话不存在或已失效"),
    AUTH_REFRESH_TOO_FREQUENT("AUTH_REFRESH_TOO_FREQUENT", "刷新过于频繁"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统异常"),
    THIRD_PARTY_ERROR("THIRD_PARTY_ERROR", "外部服务异常"),
    PAY_FAILED("PAY_FAILED", "支付失败"),
    STOCK_NOT_ENOUGH("STOCK_NOT_ENOUGH", "库存不足"),
    INVALID_VERSION("INVALID_VERSION", "API 版本号非法"),
    UNSUPPORTED_VERSION("UNSUPPORTED_VERSION", "不支持的 API 版本");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
