package com.bluecone.app.infra.wechat.openplatform;

import java.time.Instant;

/**
 * component_access_token 获取结果。
 */
public class ComponentAccessTokenResult {

    private String componentAccessToken;
    private Integer expiresIn;
    /**
     * 本地获取令牌的时间戳，便于业务计算过期时间。
     */
    private Instant obtainedAt;

    private Integer errcode;
    private String errmsg;

    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }

    public String getComponentAccessToken() {
        return componentAccessToken;
    }

    public void setComponentAccessToken(String componentAccessToken) {
        this.componentAccessToken = componentAccessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Instant getObtainedAt() {
        return obtainedAt;
    }

    public void setObtainedAt(Instant obtainedAt) {
        this.obtainedAt = obtainedAt;
    }

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }
}

