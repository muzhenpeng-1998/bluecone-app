package com.bluecone.app.api.onboarding.dto;

/**
 * 租户基础信息请求。
 * 仅用于 H5 入驻引导流程创建租户草稿。
 */
public class TenantBasicInfoRequest {

    /**
     * 入驻会话 token。
     */
    private String sessionToken;

    /**
     * 品牌名称。
     */
    private String tenantName;

    /**
     * 主体名称（公司/个体）。
     */
    private String legalName;

    /**
     * 业态标识：COFFEE / RESTAURANT 等。
     */
    private String businessCategory;

    /**
     * 渠道代码，可选；为空时从会话 channelCode 补全。
     */
    private String sourceChannel;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(String businessCategory) {
        this.businessCategory = businessCategory;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }
}

