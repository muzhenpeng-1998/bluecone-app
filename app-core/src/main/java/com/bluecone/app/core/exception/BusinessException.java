package com.bluecone.app.core.exception;

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message);
    }

    public String getCode() {
        return code;
    }
}
