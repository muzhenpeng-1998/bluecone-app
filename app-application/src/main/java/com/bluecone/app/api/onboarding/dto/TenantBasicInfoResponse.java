package com.bluecone.app.api.onboarding.dto;

/**
 * 租户基础信息响应。
 */
public class TenantBasicInfoResponse {

    /**
     * 租户主键 ID。
     */
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

