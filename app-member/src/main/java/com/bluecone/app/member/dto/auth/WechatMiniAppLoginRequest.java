package com.bluecone.app.user.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录请求。
 * 
 * Phase 3 版本：
 * - 不允许客户端传 authorizerAppId（防止串租户）
 * - 必须传 tenantId，由服务端路由到对应的 authorizerAppId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatMiniAppLoginRequest {

    /** wx.login 获取的 code（必填） */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** 租户 ID（必填，用于路由到对应的小程序） */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    /** 门店 ID（可选，用于多门店场景） */
    private Long storeId;

    /** 可选，手机号 code（推荐方式，通过 wx.getPhoneNumber 获取） */
    private String phoneCode;

    /** 可选，投放渠道标识 */
    private String sourceChannel;
}
