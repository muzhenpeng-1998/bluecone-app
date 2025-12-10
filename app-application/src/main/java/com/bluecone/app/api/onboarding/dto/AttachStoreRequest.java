package com.bluecone.app.api.onboarding.dto;

/**
 * 回填门店 ID 请求。
 */
public class AttachStoreRequest {

    /**
     * 入驻会话 token。
     */
    private String sessionToken;

    /**
     * 门店 ID。
     */
    private Long storeId;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}

