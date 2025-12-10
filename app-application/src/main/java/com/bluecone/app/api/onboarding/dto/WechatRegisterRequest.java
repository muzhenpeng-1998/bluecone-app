package com.bluecone.app.api.onboarding.dto;

/**
 * 一键开通微信小程序请求。
 */
public class WechatRegisterRequest {

    /**
     * 入驻会话 token，必填。
     */
    private String sessionToken;

    /**
     * 注册类型：FORMAL / TRIAL。
     */
    private String registerType;

    /**
     * 企业/个体工商户名称，仅在 registerType=FORMAL 时使用。
     */
    private String companyName;

    /**
     * 企业代码：统一社会信用代码 / 营业执照号，仅在 registerType=FORMAL 时使用。
     */
    private String companyCode;

    /**
     * 企业代码类型：1=统一社会信用代码，2=组织机构代码，3=营业执照注册号，仅在 registerType=FORMAL 时使用。
     */
    private Integer companyCodeType;

    /**
     * 法人微信号，仅在 registerType=FORMAL 时使用。
     */
    private String legalPersonaWechat;

    /**
     * 法人姓名，仅在 registerType=FORMAL 时使用。
     */
    private String legalPersonaName;

    /**
     * 试用小程序昵称，仅在 registerType=TRIAL 时使用。
     */
    private String trialMiniProgramName;

    /**
     * 试用小程序联系人 openid，仅在 registerType=TRIAL 时必填。
     */
    private String trialOpenId;

    /**
     * 发给微信的注册额外参数 JSON，透传字段，可选。
     */
    private String requestPayloadJson;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getRequestPayloadJson() {
        return requestPayloadJson;
    }

    public void setRequestPayloadJson(String requestPayloadJson) {
        this.requestPayloadJson = requestPayloadJson;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Integer getCompanyCodeType() {
        return companyCodeType;
    }

    public void setCompanyCodeType(Integer companyCodeType) {
        this.companyCodeType = companyCodeType;
    }

    public String getLegalPersonaWechat() {
        return legalPersonaWechat;
    }

    public void setLegalPersonaWechat(String legalPersonaWechat) {
        this.legalPersonaWechat = legalPersonaWechat;
    }

    public String getLegalPersonaName() {
        return legalPersonaName;
    }

    public void setLegalPersonaName(String legalPersonaName) {
        this.legalPersonaName = legalPersonaName;
    }

    public String getTrialMiniProgramName() {
        return trialMiniProgramName;
    }

    public void setTrialMiniProgramName(String trialMiniProgramName) {
        this.trialMiniProgramName = trialMiniProgramName;
    }

    public String getTrialOpenId() {
        return trialOpenId;
    }

    public void setTrialOpenId(String trialOpenId) {
        this.trialOpenId = trialOpenId;
    }
}
