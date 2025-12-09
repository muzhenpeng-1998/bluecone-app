package com.bluecone.app.resource.api.exception;

/**
 * 当前租户/用户无权限访问资源时抛出。
 */
public class ResourceAccessDeniedException extends ResourceException {

    private static final String DEFAULT_CODE = "RESOURCE_ACCESS_DENIED";

    public ResourceAccessDeniedException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceAccessDeniedException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }
}
