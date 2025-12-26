package com.bluecone.app.wechat.facade.miniapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序 code2session 命令。
 * <p>
 * 约束：不允许客户端传 authorizerAppId，只能通过 tenantId/storeId 路由。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatMiniAppCode2SessionCommand {

    /**
     * 租户 ID（必填）
     */
    private Long tenantId;

    /**
     * 门店 ID（可选，用于多门店场景）
     */
    private Long storeId;

    /**
     * 小程序登录凭证 code（必填，从 wx.login 获取）
     */
    private String code;
}

