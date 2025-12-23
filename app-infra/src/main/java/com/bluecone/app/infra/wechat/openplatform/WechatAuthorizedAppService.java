package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 微信已授权小程序服务。
 * <p>
 * 提供查询租户授权小程序信息的能力。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatAuthorizedAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthorizedAppService.class);

    private final WechatAuthorizedAppMapper authorizedAppMapper;

    /**
     * 根据租户 ID 查询已授权的小程序 AppID。
     * <p>
     * 如果一个租户授权了多个小程序，返回第一个（通常一个租户只授权一个小程序）。
     * </p>
     *
     * @param tenantId 租户 ID
     * @return 小程序 AppID，如果未找到返回 Optional.empty()
     */
    public Optional<String> getAuthorizerAppIdByTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("[WechatAuthorizedAppService] tenantId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("tenant_id", tenantId)
                        .eq("authorization_status", "AUTHORIZED")
                        .orderByDesc("authorized_at")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for tenantId={}", tenantId);
            return Optional.empty();
        }

        log.info("[WechatAuthorizedAppService] Found authorized app for tenantId={}, appId={}", 
                tenantId, authorizedApp.getAuthorizerAppId());
        
        return Optional.of(authorizedApp.getAuthorizerAppId());
    }

    /**
     * 根据小程序 AppID 查询授权信息。
     *
     * @param authorizerAppId 小程序 AppID
     * @return 授权信息，如果未找到返回 Optional.empty()
     */
    public Optional<WechatAuthorizedAppDO> getAuthorizedAppByAppId(String authorizerAppId) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", authorizerAppId)
                        .eq("authorization_status", "AUTHORIZED")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for appId={}", authorizerAppId);
            return Optional.empty();
        }

        return Optional.of(authorizedApp);
    }

    /**
     * 根据租户 ID 和小程序 AppID 查询授权信息。
     *
     * @param tenantId        租户 ID
     * @param authorizerAppId 小程序 AppID
     * @return 授权信息，如果未找到返回 Optional.empty()
     */
    public Optional<WechatAuthorizedAppDO> getAuthorizedApp(Long tenantId, String authorizerAppId) {
        if (tenantId == null || authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] tenantId or authorizerAppId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("tenant_id", tenantId)
                        .eq("authorizer_app_id", authorizerAppId)
                        .eq("authorization_status", "AUTHORIZED")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for tenantId={}, appId={}", 
                    tenantId, authorizerAppId);
            return Optional.empty();
        }

        return Optional.of(authorizedApp);
    }
}

