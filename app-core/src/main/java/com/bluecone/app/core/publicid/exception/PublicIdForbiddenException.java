package com.bluecone.app.core.publicid.exception;

/**
 * Public ID 权限不足异常（Scope Guard 校验失败）。
 * 
 * <p>触发场景：</p>
 * <ul>
 *   <li>租户不匹配：请求的 publicId 不属于当前租户</li>
 *   <li>门店不匹配：请求的 storeId 与上下文中的 storeId 不一致</li>
 *   <li>越权访问：尝试访问其他租户/门店的资源</li>
 * </ul>
 * 
 * <p>HTTP 映射：403 Forbidden</p>
 * <p>错误码：PUBLIC_ID_FORBIDDEN</p>
 * 
 * <p>安全建议：</p>
 * <ul>
 *   <li>错误消息不要暴露具体的 tenantId/storeId，避免信息泄露</li>
 *   <li>记录审计日志，监控潜在的越权尝试</li>
 * </ul>
 */
public class PublicIdForbiddenException extends RuntimeException {

    public PublicIdForbiddenException(String message) {
        super(message);
    }

    public PublicIdForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}

