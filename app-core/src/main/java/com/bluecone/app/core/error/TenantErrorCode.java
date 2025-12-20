package com.bluecone.app.core.error;

/**
 * 租户错误码。
 * <p>前缀 TNT，表示 Tenant 模块。</p>
 */
public enum TenantErrorCode implements ErrorCode {

    TENANT_NOT_FOUND("TENANT_NOT_FOUND", "租户不存在"),
    TENANT_DISABLED("TENANT_DISABLED", "租户已停用"),
    TENANT_EXPIRED("TENANT_EXPIRED", "租户已过期"),
    TENANT_CONTEXT_MISSING("TENANT_CONTEXT_MISSING", "上下文缺失，无法识别当前租户");

    private final String code;
    private final String message;

    TenantErrorCode(String code, String message) {
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
