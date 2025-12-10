package com.bluecone.app.api.onboarding.dto;

/**
 * 回填租户 ID 请求。
 */
public class AttachTenantRequest {

    /**
     * 入驻会话 token。
     */
    private String sessionToken;

    /**
     * 租户 ID。
     */
    private Long tenantId;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

