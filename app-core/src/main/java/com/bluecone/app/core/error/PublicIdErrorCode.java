package com.bluecone.app.core.error;

/**
 * Public ID 错误码。
 * <p>用于 Public ID 解析、治理等场景。</p>
 */
public enum PublicIdErrorCode implements ErrorCode {

    PUBLIC_ID_INVALID("PUBLIC_ID_INVALID", "Public ID 格式无效"),
    PUBLIC_ID_NOT_FOUND("PUBLIC_ID_NOT_FOUND", "Public ID 对应的资源不存在"),
    PUBLIC_ID_FORBIDDEN("PUBLIC_ID_FORBIDDEN", "没有权限访问该 Public ID"),
    PUBLIC_ID_LOOKUP_MISSING("PUBLIC_ID_LOOKUP_MISSING", "Public ID 查找配置缺失");

    private final String code;
    private final String message;

    PublicIdErrorCode(String code, String message) {
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
