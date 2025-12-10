package com.bluecone.app.api.onboarding.dto;

/**
 * 绑定平台账号请求。
 */
public class BindAccountRequest {

    /**
     * 入驻会话 token。
     */
    private String sessionToken;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 联系人手机号。
     */
    private String contactPhone;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}

