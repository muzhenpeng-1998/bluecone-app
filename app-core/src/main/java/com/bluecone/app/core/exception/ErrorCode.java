package com.bluecone.app.core.exception;

public enum ErrorCode {

    INVALID_PARAM("INVALID_PARAM", "参数非法"),
    NOT_FOUND("NOT_FOUND", "数据不存在"),
    PERMISSION_DENIED("PERMISSION_DENIED", "无权限"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token 过期"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统异常"),
    THIRD_PARTY_ERROR("THIRD_PARTY_ERROR", "外部服务异常"),
    PAY_FAILED("PAY_FAILED", "支付失败"),
    STOCK_NOT_ENOUGH("STOCK_NOT_ENOUGH", "库存不足");

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
