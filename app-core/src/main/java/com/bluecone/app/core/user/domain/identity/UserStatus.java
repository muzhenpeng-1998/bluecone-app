package com.bluecone.app.core.user.domain.identity;

/**
 * 用户状态，对应表 bc_user_identity.status。
 */
public enum UserStatus {
    ACTIVE,
    FROZEN,
    DELETED
}
