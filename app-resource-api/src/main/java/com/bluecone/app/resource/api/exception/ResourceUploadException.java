package com.bluecone.app.resource.api.exception;

/**
 * 上传流程中任意阶段失败均抛出此异常。
 */
public class ResourceUploadException extends ResourceException {

    public static final String DEFAULT_CODE = "RESOURCE_UPLOAD_ERROR";
    public static final String RES_QUOTA_DAILY_COUNT_EXCEEDED = "RES_QUOTA_DAILY_COUNT_EXCEEDED";
    public static final String RES_QUOTA_DAILY_BYTES_EXCEEDED = "RES_QUOTA_DAILY_BYTES_EXCEEDED";
    public static final String RES_UPLOAD_SESSION_EXPIRED = "RES_UPLOAD_SESSION_EXPIRED";
    public static final String RES_UPLOAD_INVALID_ARGUMENT = "RES_UPLOAD_INVALID_ARGUMENT";
    public static final String RES_UPLOAD_STORAGE_ERROR = "RES_UPLOAD_STORAGE_ERROR";

    public ResourceUploadException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceUploadException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }

    public ResourceUploadException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ResourceUploadException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
