package com.bluecone.app.core.publicid.exception;

/**
 * Public ID 未找到异常。
 * 
 * <p>触发场景：</p>
 * <ul>
 *   <li>数据库中不存在该 (tenantId, publicId) 记录</li>
 *   <li>资源已被删除（逻辑删除）</li>
 *   <li>租户隔离：publicId 存在但不属于当前租户</li>
 * </ul>
 * 
 * <p>HTTP 映射：404 Not Found</p>
 * <p>错误码：PUBLIC_ID_NOT_FOUND</p>
 */
public class PublicIdNotFoundException extends RuntimeException {

    public PublicIdNotFoundException(String message) {
        super(message);
    }

    public PublicIdNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

