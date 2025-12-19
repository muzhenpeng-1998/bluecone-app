package com.bluecone.app.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import com.bluecone.app.infra.tenant.mapper.TenantOnboardingSessionMapper;
import com.bluecone.app.tenant.service.TenantOnboardingAppService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 租户入驻引导会话应用服务实现。
 *
 * 仅负责维护会话表 bc_tenant_onboarding_session，不直接创建租户/门店。
 */
@Service
@RequiredArgsConstructor
public class TenantOnboardingAppServiceImpl implements TenantOnboardingAppService {

    private final TenantOnboardingSessionMapper sessionMapper;

    /**
     * 创建一条新的入驻会话记录，并返回会话 token。
     *
     * @param channelCode 渠道代码，可为空
     * @return 生成的会话 token，前端用于后续步骤关联
     */
    @Override
    @Transactional
    public String startSession(String channelCode) {
        String sessionToken = generateSessionToken();

        TenantOnboardingSessionDO session = new TenantOnboardingSessionDO();
        session.setSessionToken(sessionToken);
        session.setChannelCode(channelCode);
        // 初始状态：0-初始化
        session.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        sessionMapper.insert(session);
        return sessionToken;
    }

    /**
     * 绑定平台用户信息到会话。
     *
     * @param sessionToken 会话 token
     * @param userId       平台用户 ID
     * @param contactPhone 联系人手机号
     */
    @Override
    @Transactional
    public void attachUser(String sessionToken, Long userId, String contactPhone) {
        TenantOnboardingSessionDO session = requireSession(sessionToken);
        session.setUserId(userId);
        if (StringUtils.hasText(contactPhone)) {
            session.setContactPhone(contactPhone);
        }
        // 状态至少置为 1-填写中
        if (session.getStatus() == null || session.getStatus() < 1) {
            session.setStatus(1);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    /**
     * 将租户 ID 绑定到会话。
     *
     * @param sessionToken 会话 token
     * @param tenantId     租户 ID
     */
    @Override
    @Transactional
    public void attachTenant(String sessionToken, Long tenantId) {
        TenantOnboardingSessionDO session = requireSession(sessionToken);
        session.setTenantId(tenantId);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    /**
     * 将门店 ID 绑定到会话。
     *
     * @param sessionToken 会话 token
     * @param storeId      门店 ID
     */
    @Override
    @Transactional
    public void attachStore(String sessionToken, Long storeId) {
        TenantOnboardingSessionDO session = requireSession(sessionToken);
        session.setStoreId(storeId);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    /**
     * 按会话 token 查询入驻会话详情。
     *
     * @param sessionToken 会话 token
     * @return 会话数据对象，如不存在返回 null
     */
    @Override
    @Transactional(readOnly = true)
    public TenantOnboardingSessionDO findBySessionToken(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            return null;
        }
        LambdaQueryWrapper<TenantOnboardingSessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TenantOnboardingSessionDO::getSessionToken, sessionToken);
        return sessionMapper.selectOne(wrapper);
    }

    private TenantOnboardingSessionDO requireSession(String sessionToken) {
        TenantOnboardingSessionDO session = findBySessionToken(sessionToken);
        if (session == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "入驻会话不存在或已失效");
        }
        return session;
    }

    private String generateSessionToken() {
        // 使用 UUID 生成高概率唯一的会话 token，去掉连字符便于前端存储
        return UUID.randomUUID().toString().replace("-", "");
    }
}
