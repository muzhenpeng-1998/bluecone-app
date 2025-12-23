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
     * 基于微信小程序 openId 注册/加载用户（支持 unionId 为空的场景）。
     * 
     * 识别优先级：
     * 1. unionId（如果不为空）
     * 2. phone + countryCode（如果不为空）
     * 3. external_identity (appId, openId)（兜底）
     * 
     * @param unionId         微信 unionId（可为空）
     * @param appId           小程序 appId（必填）
     * @param openId          小程序 openId（必填）
     * @param phone           手机号（可为空）
     * @param countryCode     国家区号（可为空）
     * @param firstTenantId   首次注册租户ID
     * @param registerChannel 注册渠道
     * @return 用户注册结果
     */
    UserRegistrationResult registerOrLoadByWeChatMiniApp(String unionId,
                                                         String appId,
                                                         String openId,
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
