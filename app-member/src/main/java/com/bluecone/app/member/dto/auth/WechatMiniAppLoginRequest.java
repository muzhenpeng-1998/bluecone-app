package com.bluecone.app.user.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录请求。
 * 
 * 注意：不再信任客户端传 tenantId，改为以 authorizerAppId（小程序 appId）为主键，
 * 从 bc_wechat_authorized_app 反查 tenantId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatMiniAppLoginRequest {

    /** wx.login 获取的 code（必填） */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** 必填，小程序自身 appId（通过 wx.getAccountInfoSync().miniProgram.appId 获取） */
    @NotBlank(message = "authorizerAppId 不能为空")
    private String authorizerAppId;

    /** 可选，手机号 code（推荐方式，通过 wx.getPhoneNumber 获取） */
    private String phoneCode;

    /** 可选，加密数据（兼容旧版本，如手机号） */
    private String encryptedData;

    /** 可选，对应 encryptedData 的 IV（兼容旧版本） */
    private String iv;

    /** 可选，投放渠道标识 */
    private String sourceChannel;
}
