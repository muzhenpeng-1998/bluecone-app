package com.bluecone.app.infra.wechat.openplatform;

/**
 * 授权方（小程序）基础信息。
 */
public class AuthorizerInfoResult {

    private Integer errcode;
    private String errmsg;

    private String authorizerAppId;
    private String nickName;
    private String principalName;
    private String headImg;
    private String signature;
    /**
     * 是否认证，保留微信返回的整数编码。
     */
    private Integer verifyType;

    /**
     * 账号主体类型：个人 / 企业 / 媒体 / 政府 / 其他。
     * 这里只保留一个枚举值的 int，具体含义留给业务层解释。
     */
    private Integer principalType;

    public boolean isSuccess() {
        return errcode == null || errcode == 0;
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

    public String getAuthorizerAppId() {
        return authorizerAppId;
    }

    public void setAuthorizerAppId(String authorizerAppId) {
        this.authorizerAppId = authorizerAppId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getHeadImg() {
        return headImg;
    }

    public void setHeadImg(String headImg) {
        this.headImg = headImg;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Integer getVerifyType() {
        return verifyType;
    }

    public void setVerifyType(Integer verifyType) {
        this.verifyType = verifyType;
    }

    public Integer getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(Integer principalType) {
        this.principalType = principalType;
    }
}

