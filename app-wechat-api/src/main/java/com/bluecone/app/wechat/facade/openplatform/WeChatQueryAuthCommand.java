package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * queryAuth 命令（用于浏览器授权回调）。
 * <p>
 * 使用授权码换取授权信息和 tokens。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatQueryAuthCommand {

    /**
     * 授权码（auth_code，从微信授权回调 URL 中获取）
     */
    private String authCode;

    /**
     * 租户 ID（可选，用于绑定租户）
     */
    private Long tenantId;

    /**
     * 门店 ID（可选）
     */
    private Long storeId;
}

