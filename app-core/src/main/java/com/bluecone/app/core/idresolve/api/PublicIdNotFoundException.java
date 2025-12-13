package com.bluecone.app.core.idresolve.api;

/**
 * 公共 ID 未找到时抛出的异常，通常映射为 404 或业务层的 NOT_FOUND 错误码。
 */
public class PublicIdNotFoundException extends RuntimeException {

    public PublicIdNotFoundException(String message) {
        super(message);
    }
}

