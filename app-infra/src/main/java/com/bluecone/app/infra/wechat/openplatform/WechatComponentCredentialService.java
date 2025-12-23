package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信开放平台组件凭证管理服务。
 * <p>
 * 负责持久化 component_verify_ticket 与 component_access_token，并提供统一的读取与刷新能力。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatComponentCredentialService {

    private static final Logger log = LoggerFactory.getLogger(WechatComponentCredentialService.class);

    private final WechatComponentCredentialMapper credentialMapper;
    private final WeChatOpenPlatformClient weChatOpenPlatformClient;

    @Value("${wechat.open-platform.component-app-id:}")
    private String componentAppId;

    @Value("${wechat.open-platform.component-app-secret:}")
    private String componentAppSecret;

    /**
     * 保存或更新 component_verify_ticket。
     *
     * @param componentVerifyTicket 最新的 component_verify_ticket
     */
    @Transactional
    public void saveOrUpdateVerifyTicket(String componentVerifyTicket) {
        if (!StringUtils.hasText(componentAppId)) {
            log.error("[WechatComponentCredential] componentAppId 未配置，无法保存 verify_ticket");
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        WechatComponentCredentialDO existing = credentialMapper.selectOne(
                new QueryWrapper<WechatComponentCredentialDO>()
                        .eq("component_app_id", componentAppId)
                        .last("LIMIT 1"));

        if (existing == null) {
            WechatComponentCredentialDO entity = new WechatComponentCredentialDO();
            entity.setComponentAppId(componentAppId);
            entity.setComponentAppSecret(componentAppSecret);
            entity.setComponentVerifyTicket(componentVerifyTicket);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            credentialMapper.insert(entity);
            log.info("[WechatComponentCredential] created credential record for componentAppId={}", componentAppId);
        } else {
            existing.setComponentVerifyTicket(componentVerifyTicket);
            existing.setUpdatedAt(now);
            credentialMapper.updateById(existing);
            log.info("[WechatComponentCredential] updated verify_ticket for componentAppId={}", componentAppId);
        }
    }

    /**
     * 获取当前有效的 component_access_token。
     * <p>
     * 如本地缓存的 token 仍在有效期内，直接返回；否则调用微信开放平台接口刷新并持久化。
     * </p>
     *
     * @return 有效的 component_access_token
     */
    @Transactional
    public String getValidComponentAccessToken() {
        if (!StringUtils.hasText(componentAppId) || !StringUtils.hasText(componentAppSecret)) {
            log.error("[WechatComponentCredential] componentAppId 或 componentAppSecret 未配置，无法获取 component_access_token");
            throw new IllegalStateException("wechat open platform component config missing");
        }

        WechatComponentCredentialDO credential = credentialMapper.selectOne(
                new QueryWrapper<WechatComponentCredentialDO>()
                        .eq("component_app_id", componentAppId)
                        .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (credential != null
                && StringUtils.hasText(credential.getComponentAccessToken())
                && credential.getComponentAccessTokenExpireAt() != null
                && credential.getComponentAccessTokenExpireAt().isAfter(now.plusMinutes(2))) {
            return credential.getComponentAccessToken();
        }

        if (credential == null || !StringUtils.hasText(credential.getComponentVerifyTicket())) {
            log.error("[WechatComponentCredential] component_verify_ticket 为空，无法刷新 component_access_token，componentAppId={}",
                    componentAppId);
            throw new IllegalStateException("component_verify_ticket missing, cannot refresh component_access_token");
        }

        ComponentAccessTokenResult result = weChatOpenPlatformClient.getComponentAccessToken(
                componentAppId,
                componentAppSecret,
                credential.getComponentVerifyTicket());
        if (result == null || !result.isSuccess() || !StringUtils.hasText(result.getComponentAccessToken())) {
            log.error("[WechatComponentCredential] getComponentAccessToken failed, componentAppId={}, errcode={}, errmsg={}",
                    componentAppId,
                    result != null ? result.getErrcode() : null,
                    result != null ? result.getErrmsg() : null);
            throw new IllegalStateException("failed to fetch component_access_token from WeChat");
        }

        String newToken = result.getComponentAccessToken();
        Integer expiresIn = result.getExpiresIn() != null ? result.getExpiresIn() : 7200;
        LocalDateTime expiresAt = now.plusSeconds(expiresIn - 120L);

        credential.setComponentAccessToken(newToken);
        credential.setComponentAccessTokenExpireAt(expiresAt);
        credential.setUpdatedAt(now);
        credentialMapper.updateById(credential);

        log.info("[WechatComponentCredential] refreshed component_access_token for componentAppId={}, expiresIn={}s",
                componentAppId, expiresIn);
        return newToken;
    }

    /**
     * 获取当前组件凭证记录（只读，用于调试或监控）。
     *
     * @return 当前组件凭证记录，可能为 null
     */
    @Transactional(readOnly = true)
    public WechatComponentCredentialDO getCurrentCredential() {
        if (!StringUtils.hasText(componentAppId)) {
            return null;
        }
        return credentialMapper.selectOne(
                new QueryWrapper<WechatComponentCredentialDO>()
                        .eq("component_app_id", componentAppId)
                        .last("LIMIT 1"));
    }
}
