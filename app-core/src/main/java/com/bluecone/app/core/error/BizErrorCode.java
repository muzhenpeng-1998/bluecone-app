package com.bluecone.app.core.error;

/**
 * 业务通用错误码，供应用层统一使用。
 */
public enum BizErrorCode implements ErrorCode {

    CONTEXT_MISSING("BIZ-401-001", "上下文缺失，无法识别当前租户"),
    RESOURCE_NOT_FOUND("BIZ-404-001", "资源不存在"),
    PERMISSION_DENIED("BIZ-403-001", "没有权限执行该操作"),
    INVALID_PARAM("BIZ-400-001", "请求参数非法");

    private final String code;
    private final String message;

    BizErrorCode(String code, String message) {
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
