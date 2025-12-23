package com.bluecone.app.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatMiniAppLoginRequest {

    /** wx.login 获取的 code */
    private String code;

    /** 可选，手机号 code（推荐方式，通过 wx.getPhoneNumber 获取） */
    private String phoneCode;

    /** 可选，加密数据（兼容旧版本，如手机号） */
    private String encryptedData;

    /** 可选，对应 encryptedData 的 IV（兼容旧版本） */
    private String iv;

    /** 必填，来源租户 ID */
    private Long sourceTenantId;

    /** 可选，投放渠道标识 */
    private String sourceChannel;
}
