package com.bluecone.app.resource.api.exception;

/**
 * 上传流程中任意阶段失败均抛出此异常。
 */
public class ResourceUploadException extends ResourceException {

    private static final String DEFAULT_CODE = "RESOURCE_UPLOAD_ERROR";

    public ResourceUploadException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceUploadException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }
}
