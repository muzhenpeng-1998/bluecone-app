package com.bluecone.app.core.exception;

/**
 * 老版错误码枚举（已废弃）。
 * 
 * @deprecated 请使用新版错误码体系：
 * <ul>
 *   <li>{@link com.bluecone.app.core.error.AuthErrorCode} - 认证/授权错误</li>
 *   <li>{@link com.bluecone.app.core.error.ParamErrorCode} - 参数错误</li>
 *   <li>{@link com.bluecone.app.core.error.CommonErrorCode} - 系统通用错误</li>
 *   <li>{@link com.bluecone.app.core.error.UserErrorCode} - 用户模块错误</li>
 *   <li>{@link com.bluecone.app.core.error.TenantErrorCode} - 租户模块错误</li>
 * </ul>
 * 
 * 新版错误码统一实现 {@link com.bluecone.app.core.error.ErrorCode} 接口，
 * 支持按领域拆分，便于维护与扩展。
 */
@Deprecated
public enum ErrorCode {

    INVALID_PARAM("INVALID_PARAM", "参数非法"),
    PARAM_MISSING("PARAM_MISSING", "缺少必需参数"),
    PARAM_INVALID("PARAM_INVALID", "参数值非法"),
    NOT_FOUND("NOT_FOUND", "数据不存在"),
    PERMISSION_DENIED("PERMISSION_DENIED", "无权限"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token 过期"),
    AUTH_REQUIRED("AUTH_REQUIRED", "未登录或登录已过期"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "用户名或密码错误"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "凭证无效或已过期"),
    AUTH_SESSION_INVALID("AUTH_SESSION_INVALID", "会话不存在或已失效"),
    AUTH_REFRESH_TOO_FREQUENT("AUTH_REFRESH_TOO_FREQUENT", "刷新过于频繁"),
    UNAUTHORIZED("UNAUTHORIZED", "未授权的访问"),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁"),
    IDEMPOTENT_REJECTED("IDEMPOTENT_REJECTED", "请求重复提交"),
    SIGNATURE_INVALID("SIGNATURE_INVALID", "签名校验失败"),
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
