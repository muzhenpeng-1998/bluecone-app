package com.bluecone.app.core.error;

/**
 * 参数错误码。
 * <p>用于参数校验、参数绑定等场景。</p>
 */
public enum ParamErrorCode implements ErrorCode {

    INVALID_PARAM("INVALID_PARAM", "参数非法"),
    MISSING_PARAM("MISSING_PARAM", "缺少必需参数"),
    PARAM_INVALID("PARAM_INVALID", "参数值非法"),
    PARAM_MISSING("PARAM_MISSING", "缺少必需参数");

    private final String code;
    private final String message;

    ParamErrorCode(String code, String message) {
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
