package com.bluecone.app.user.application;

/**
 * 当前用户上下文抽象，屏蔽具体安全实现。
 */
public interface CurrentUserContext {

    /**
     * 获取当前登录用户 ID。
     */
    Long getCurrentUserId();

    /**
     * 获取当前租户 ID（如有）。
     */
    Long getCurrentTenantId();

    /**
     * 获取当前会员 ID，如未开通则返回 null。
     */
    Long getCurrentMemberIdOrNull();
}
