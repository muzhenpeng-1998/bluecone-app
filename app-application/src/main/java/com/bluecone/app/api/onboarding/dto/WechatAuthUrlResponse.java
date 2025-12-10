package com.bluecone.app.api.onboarding.dto;

/**
 * 微信开放平台授权 URL 响应。
 */
public class WechatAuthUrlResponse {

    /**
     * 前端直接跳转的微信授权地址。
     */
    private String authorizeUrl;

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }
}

