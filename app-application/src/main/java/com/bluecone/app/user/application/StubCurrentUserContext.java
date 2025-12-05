package com.bluecone.app.user.application;

import org.springframework.stereotype.Component;

/**
 * 占位的当前用户上下文实现，后续接入安全体系时替换。
 */
@Component
public class StubCurrentUserContext implements CurrentUserContext {

    @Override
    public Long getCurrentUserId() {
        // TODO: 从实际安全上下文获取 userId
        return null;
    }

    @Override
    public Long getCurrentTenantId() {
        // TODO: 从实际安全上下文获取 tenantId
        return null;
    }

    @Override
    public Long getCurrentMemberIdOrNull() {
        // TODO: 从实际安全上下文获取 memberId
        return null;
    }
}
