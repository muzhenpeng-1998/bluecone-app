package com.bluecone.app.api.auth.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应。
 */
@Data
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpireAt;
    private LocalDateTime refreshTokenExpireAt;
    private String sessionId;
}
