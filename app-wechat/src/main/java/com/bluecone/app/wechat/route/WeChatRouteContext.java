package com.bluecone.app.wechat.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 微信路由上下文。
 * <p>
 * 根据 tenantId/storeId 路由到对应的微信运行时上下文（authorizerAppId 等）。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatRouteContext {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 门店 ID（可选）
     */
    private Long storeId;

    /**
     * 授权方 AppID（= subAppId，小程序的 appId）
     */
    private String authorizerAppId;

    /**
     * 授权状态（AUTHORIZED / UNAUTHORIZED）
     */
    private String authorizationStatus;

    /**
     * 授权时间
     */
    private LocalDateTime authorizedAt;

    /**
     * 最近更新时间
     */
    private LocalDateTime updatedAt;
}

