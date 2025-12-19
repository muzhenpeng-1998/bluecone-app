package com.bluecone.app.core.exception;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 统一业务异常。
 * <p>携带错误码与提示信息，便于全局异常处理器统一返回。</p>
 */
public class BusinessException extends RuntimeException {

    private final String code;

    /**
     * 构造业务异常（使用 ErrorCode）
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage(), null, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（使用 ErrorCode + 自定义消息）
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（使用 ErrorCode + 自定义消息 + 原因）
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause, false, false);
        this.code = errorCode.getCode();
    }

    /**
     * 构造业务异常（直接指定 code 和 message）
     */
    public BusinessException(String code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    /**
     * 静态工厂方法
     */
    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message);
    }

    public String getCode() {
        return code;
    }
}
