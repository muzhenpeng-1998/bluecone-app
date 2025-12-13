package com.bluecone.app.core.error;

/**
 * 用户模块错误码定义。
 */
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USR-404-001", "用户不存在"),
    USER_FROZEN("USR-403-001", "用户已冻结"),
    USER_DELETED("USR-410-001", "用户已注销");

    private final String code;
    private final String message;

    UserErrorCode(String code, String message) {
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

