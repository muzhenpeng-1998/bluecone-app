package com.bluecone.app.resource.api.exception;

/**
 * 在未能找到资源或绑定关系时抛出。
 */
public class ResourceNotFoundException extends ResourceException {

    private static final String DEFAULT_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(DEFAULT_CODE, message, cause);
    }
}
