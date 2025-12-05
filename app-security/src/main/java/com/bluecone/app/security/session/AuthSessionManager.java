package com.bluecone.app.security.session;

/**
 * 会话管理接口，封装 access/refresh token 生命周期。
 */
public interface AuthSessionManager {

    AuthSessionCreateResult createSession(Long userId,
                                          Long tenantId,
                                          String clientType,
                                          String deviceId,
                                          String ipAddress,
                                          String userAgent);

    AuthSessionCreateResult refreshSession(String refreshToken);

    void revokeSession(String accessToken);
}
