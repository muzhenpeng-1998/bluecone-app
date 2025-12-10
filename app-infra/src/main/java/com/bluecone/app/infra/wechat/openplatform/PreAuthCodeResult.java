package com.bluecone.app.infra.wechat.openplatform;

import java.time.Instant;

/**
 * 预授权码 pre_auth_code 结果。
 */
public class PreAuthCodeResult {

    private String preAuthCode;
    private Integer expiresIn;
    private Instant obtainedAt;

    private Integer errcode;
    private String errmsg;

    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }

    public String getPreAuthCode() {
        return preAuthCode;
    }

    public void setPreAuthCode(String preAuthCode) {
        this.preAuthCode = preAuthCode;
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

