package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预授权链接生成命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPreAuthUrlCommand {

    /**
     * 租户 ID（必填）
     */
    private Long tenantId;

    /**
     * 门店 ID（可选）
     */
    private Long storeId;

    /**
     * 授权完成后的回调地址（可选，默认使用系统配置）
     */
    private String redirectUri;

    /**
     * 自定义参数（可选，会附加在回调 URL 中，如 sessionToken）
     */
    private String customParam;
}

