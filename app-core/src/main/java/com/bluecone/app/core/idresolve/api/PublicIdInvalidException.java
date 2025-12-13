package com.bluecone.app.core.idresolve.api;

/**
 * 公共 ID 格式非法或前缀不匹配时抛出的异常，通常映射为 400。
 */
public class PublicIdInvalidException extends RuntimeException {

    public PublicIdInvalidException(String message) {
        super(message);
    }
}

