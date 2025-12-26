package com.bluecone.app.wechat.route;

import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppDO;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 微信路由服务。
 * <p>
 * 职责：根据 tenantId/storeId 路由到对应的微信运行时上下文（authorizerAppId 等）。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WeChatRouteService {

    private static final Logger log = LoggerFactory.getLogger(WeChatRouteService.class);

    private final WechatAuthorizedAppService wechatAuthorizedAppService;

    /**
     * 解析路由上下文。
     * <p>
     * 根据 tenantId/storeId 查询授权信息，返回 authorizerAppId 等运行时上下文。
     * </p>
     *
     * @param tenantId 租户 ID（必填）
     * @param storeId  门店 ID（可选，当前版本暂不使用）
     * @return 路由上下文
     * @throws IllegalStateException 如果未找到授权信息或授权状态异常
     */
    public WeChatRouteContext resolve(Long tenantId, Long storeId) {
        if (tenantId == null) {
            log.error("[WeChatRouteService] tenantId is null");
            throw new IllegalArgumentException("tenantId 不能为空");
        }

        log.debug("[WeChatRouteService] resolve route, tenantId={}, storeId={}", tenantId, storeId);

        // 根据 tenantId 查询授权信息
        WechatAuthorizedAppDO authorizedApp = wechatAuthorizedAppService
                .getAuthorizedAppByAppId(
                        wechatAuthorizedAppService.getAuthorizerAppIdByTenantId(tenantId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "租户未授权小程序，tenantId=" + tenantId))
                )
                .orElseThrow(() -> new IllegalStateException(
                        "租户授权信息异常，tenantId=" + tenantId));

        // 检查授权状态
        if (!"AUTHORIZED".equals(authorizedApp.getAuthorizationStatus())) {
            log.error("[WeChatRouteService] 授权状态异常, tenantId={}, status={}",
                    tenantId, authorizedApp.getAuthorizationStatus());
            throw new IllegalStateException(
                    "小程序授权状态异常，tenantId=" + tenantId + ", status=" + authorizedApp.getAuthorizationStatus());
        }

        String authorizerAppId = authorizedApp.getAuthorizerAppId();
        log.info("[WeChatRouteService] resolved route, tenantId={}, authorizerAppId={}***",
                tenantId, maskAppId(authorizerAppId));

        return WeChatRouteContext.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .authorizerAppId(authorizerAppId)
                .authorizationStatus(authorizedApp.getAuthorizationStatus())
                .authorizedAt(authorizedApp.getAuthorizedAt())
                .updatedAt(authorizedApp.getUpdatedAt())
                .build();
    }

    /**
     * 脱敏 AppID（只显示前 6 位）
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 6) {
            return "***";
        }
        return appId.substring(0, 6);
    }
}

