package com.bluecone.app.infra.security.token;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 中携带的用户上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUserContext {

    private String tokenId;
    private Long userId;
    private Long tenantId;
    private String sessionId;
    private String clientType;
    private String deviceId;
    private LocalDateTime issuedAt;
    private LocalDateTime expireAt;
}
