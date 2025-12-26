package com.bluecone.app.wechat.facade.miniapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序手机号获取结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatMiniAppPhoneResult {

    /**
     * 手机号（带国家码，如 +86）
     */
    private String phoneNumber;

    /**
     * 国家码（如 86）
     */
    private String countryCode;

    /**
     * 纯手机号（不带国家码）
     */
    private String purePhoneNumber;
}

