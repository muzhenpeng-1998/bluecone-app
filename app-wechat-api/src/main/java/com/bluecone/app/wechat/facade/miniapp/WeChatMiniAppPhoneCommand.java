package com.bluecone.app.wechat.facade.miniapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序手机号获取命令。
 * <p>
 * 约束：不允许客户端传 authorizerAppId，只能通过 tenantId/storeId 路由。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatMiniAppPhoneCommand {

    /**
     * 租户 ID（必填）
     */
    private Long tenantId;

    /**
     * 门店 ID（可选，用于多门店场景）
     */
    private Long storeId;

    /**
     * 手机号获取凭证（必填，从 wx.getPhoneNumber 获取）
     */
    private String phoneCode;
}

