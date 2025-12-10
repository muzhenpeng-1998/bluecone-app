package com.bluecone.app.api.onboarding.dto;

/**
 * 创建入驻会话响应。
 */
public class StartSessionResponse {

    /**
     * 会话 token，前端需保存用于后续步骤。
     */
    private String sessionToken;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}

