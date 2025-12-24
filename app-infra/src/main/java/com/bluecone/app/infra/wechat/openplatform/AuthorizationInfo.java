package com.bluecone.app.infra.wechat.openplatform;

import java.util.List;

/**
 * 授权信息（微信开放平台 query_auth 返回的 authorization_info 部分）。
 * <p>
 * 对应微信文档：https://developers.weixin.qq.com/doc/oplatform/openApi/OpenApiDoc/authorization-management/getAuthorizerInfo.html
 * </p>
 */
public class AuthorizationInfo {

    /**
     * 授权方 appid
     */
    private String authorizerAppid;

    /**
     * 授权方接口调用令牌（在授权的公众号/小程序具备 API 权限时，才有此返回值）
     */
    private String authorizerAccessToken;

    /**
     * 有效期（秒）
     */
    private Integer expiresIn;

    /**
     * 授权方的刷新令牌（在授权的公众号/小程序具备 API 权限时，才有此返回值）
     */
    private String authorizerRefreshToken;

    /**
     * 授权给开发者的权限集列表
     * <p>
     * 每个元素是一个权限信息对象，包含 funcscope_category 字段
     * 为简化，这里直接存储 funcscope_category 的 id 列表
     * </p>
     */
    private List<Integer> funcInfo;

    public String getAuthorizerAppid() {
        return authorizerAppid;
    }

    public void setAuthorizerAppid(String authorizerAppid) {
        this.authorizerAppid = authorizerAppid;
    }

    public String getAuthorizerAccessToken() {
        return authorizerAccessToken;
    }

    public void setAuthorizerAccessToken(String authorizerAccessToken) {
        this.authorizerAccessToken = authorizerAccessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getAuthorizerRefreshToken() {
        return authorizerRefreshToken;
    }

    public void setAuthorizerRefreshToken(String authorizerRefreshToken) {
        this.authorizerRefreshToken = authorizerRefreshToken;
    }

    public List<Integer> getFuncInfo() {
        return funcInfo;
    }

    public void setFuncInfo(List<Integer> funcInfo) {
        this.funcInfo = funcInfo;
    }
}

