package com.bluecone.app.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信小程序登录命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByWeChatMiniAppCommand {

    /** wx.login 获取的 code */
    private String code;

    /** 可选，加密数据（如手机号） */
    private String encryptedData;

    /** 可选，对应 encryptedData 的 IV */
    private String iv;

    /** 可选，来源租户 */
    private Long sourceTenantId;

    /** 可选，投放渠道标识 */
    private String sourceChannel;
}
