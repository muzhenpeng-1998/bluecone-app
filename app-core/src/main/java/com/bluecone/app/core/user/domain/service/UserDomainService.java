package com.bluecone.app.core.user.domain.service;

import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.profile.UserProfile;

/**
 * 用户领域服务，封装注册与画像初始化逻辑。
 */
public interface UserDomainService {

    /**
     * 基于微信 UnionId 或手机号注册/加载用户。
     */
    UserRegistrationResult registerOrLoadByWeChatUnionId(String unionId,
                                                         String phone,
                                                         String countryCode,
                                                         Long firstTenantId,
                                                         RegisterChannel registerChannel);

    /**
     * 初始化或更新用户画像。
     */
    UserProfile initOrUpdateProfile(Long userId, UserProfile profileInput);

    record UserRegistrationResult(UserIdentity identity, boolean isNew) {
    }
}
