package com.bluecone.app.core.exception;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 统一业务异常。
 * <p>携带错误码与提示信息，便于全局异常处理器统一返回。</p>
 */
public class BizException extends RuntimeException {

    private final String code;
    private final String message;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
