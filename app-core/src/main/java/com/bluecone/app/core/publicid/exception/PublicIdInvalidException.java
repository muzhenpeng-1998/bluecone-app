package com.bluecone.app.core.publicid.exception;

/**
 * Public ID 格式非法或前缀不匹配异常。
 * 
 * <p>触发场景：</p>
 * <ul>
 *   <li>格式错误：缺少下划线分隔符、ULID 长度不对</li>
 *   <li>前缀不匹配：期望 sto_ 开头，实际传入 prd_</li>
 *   <li>字符非法：ULID 包含非 Crockford Base32 字符</li>
 * </ul>
 * 
 * <p>HTTP 映射：400 Bad Request</p>
 * <p>错误码：PUBLIC_ID_INVALID</p>
 */
public class PublicIdInvalidException extends RuntimeException {

    public PublicIdInvalidException(String message) {
        super(message);
    }

    public PublicIdInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}

