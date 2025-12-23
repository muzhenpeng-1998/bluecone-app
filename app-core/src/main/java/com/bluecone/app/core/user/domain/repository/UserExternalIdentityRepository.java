package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

/**
 * 用户外部身份绑定仓储接口。
 * 
 * 用于管理微信 openId、支付宝 userId 等外部身份标识的绑定关系。
 * 当 unionId 为空时，使用 (provider, appId, openId) 作为兜底唯一标识。
 */
public interface UserExternalIdentityRepository {

    /**
     * 根据微信小程序 openId 查找平台用户ID。
     *
     * @param appId  小程序 appId
     * @param openId 小程序 openId
     * @return 平台用户ID（如果存在）
     */
    Optional<Long> findUserIdByWeChatOpenId(String appId, String openId);

    /**
     * 绑定微信小程序 openId 到平台用户。
     * 
     * 幂等操作：如果已绑定则不重复插入。
     *
     * @param userId  平台用户ID
     * @param appId   小程序 appId
     * @param openId  小程序 openId
     * @param unionId 微信 unionId（可为空）
     */
    void bindWeChatOpenId(long userId, String appId, String openId, String unionId);

    /**
     * 根据外部身份标识查找平台用户ID（通用方法）。
     *
     * @param provider 身份提供方（如 WECHAT_MINI、WECHAT_H5、ALIPAY）
     * @param appId    外部应用ID
     * @param openId   外部用户ID
     * @return 平台用户ID（如果存在）
     */
    Optional<Long> findUserIdByExternalIdentity(String provider, String appId, String openId);

    /**
     * 绑定外部身份到平台用户（通用方法）。
     * 
     * 幂等操作：如果已绑定则不重复插入。
     *
     * @param userId   平台用户ID
     * @param provider 身份提供方
     * @param appId    外部应用ID
     * @param openId   外部用户ID
     * @param unionId  外部 UnionId（可为空）
     */
    void bindExternalIdentity(long userId, String provider, String appId, String openId, String unionId);
}

