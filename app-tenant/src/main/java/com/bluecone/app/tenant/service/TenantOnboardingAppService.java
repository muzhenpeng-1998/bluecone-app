package com.bluecone.app.tenant.service;

import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;

/**
 * 租户入驻引导会话应用服务。
 *
 * 负责创建和维护 bc_tenant_onboarding_session 会话生命周期，
 * 不直接创建租户/门店，仅保存会话中的关联 ID 与状态。
 */
public interface TenantOnboardingAppService {

    /**
     * 创建一条新的入驻会话记录，并返回会话 token。
     *
     * @param channelCode 渠道代码，可为空
     * @return 生成的会话 token，前端用于后续步骤关联
     */
    String startSession(String channelCode);

    /**
     * 绑定平台用户信息到会话。
     *
     * @param sessionToken 会话 token
     * @param userId       平台用户 ID
     * @param contactPhone 联系人手机号
     */
    void attachUser(String sessionToken, Long userId, String contactPhone);

    /**
     * 将租户 ID 绑定到会话。
     *
     * @param sessionToken 会话 token
     * @param tenantId     租户 ID
     */
    void attachTenant(String sessionToken, Long tenantId);

    /**
     * 将门店 ID 绑定到会话。
     *
     * @param sessionToken 会话 token
     * @param storeId      门店 ID
     */
    void attachStore(String sessionToken, Long storeId);

    /**
     * 按会话 token 查询入驻会话详情。
     *
     * @param sessionToken 会话 token
     * @return 会话数据对象，如不存在返回 null
     */
    TenantOnboardingSessionDO findBySessionToken(String sessionToken);
}

