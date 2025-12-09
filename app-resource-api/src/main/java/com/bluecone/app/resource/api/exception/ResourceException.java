package com.bluecone.app.resource.api.exception;

/**
 * 资源中心业务异常基类，可承载业务码与提示信息。
 */
public abstract class ResourceException extends RuntimeException {

    private final String errorCode;

    protected ResourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ResourceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 返回业务错误码。
     */
    public String getErrorCode() {
        return errorCode;
    }
}
