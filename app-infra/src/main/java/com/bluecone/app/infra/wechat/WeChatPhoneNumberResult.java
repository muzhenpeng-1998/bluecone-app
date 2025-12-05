package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * 微信手机号解密结果。
 */
@Data
public class WeChatPhoneNumberResult {

    private String phoneNumber;

    private String countryCode;
}
