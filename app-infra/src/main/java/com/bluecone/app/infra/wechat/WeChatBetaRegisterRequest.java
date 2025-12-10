package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * fastregisterbetaweapp 请求入参。
 *
 * 微信官方接口典型参数为 name + openid。
 */
@Data
public class WeChatBetaRegisterRequest {

    /**
     * 试用小程序昵称（商户品牌名，例如“Dont Worry Coffee”）。
     */
    private String name;

    /**
     * 商户联系人对应的微信 OpenID。
     * 一般由业务层通过网页授权获取。
     */
    private String openId;

    /**
     * 预留扩展字段，保存额外参数（如地区、类目等）。
     */
    private String extJson;
}

