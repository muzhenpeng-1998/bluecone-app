package com.bluecone.app.core.error;

/**
 * 认证/授权错误码。
 * <p>前缀 AUTH，表示认证授权模块。</p>
 */
public enum AuthErrorCode implements ErrorCode {

    AUTH_REQUIRED("AUTH_REQUIRED", "未登录或登录已过期"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token 过期"),
    TOKEN_INVALID("TOKEN_INVALID", "Token 无效"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "用户名或密码错误"),
    AUTH_SESSION_INVALID("AUTH_SESSION_INVALID", "会话不存在或已失效"),
    AUTH_REFRESH_TOO_FREQUENT("AUTH_REFRESH_TOO_FREQUENT", "刷新过于频繁"),
    PERMISSION_DENIED("PERMISSION_DENIED", "没有权限执行该操作"),
    UNAUTHORIZED("UNAUTHORIZED", "未授权的访问");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
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
