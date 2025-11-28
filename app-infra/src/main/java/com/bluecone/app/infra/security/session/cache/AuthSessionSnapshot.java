package com.bluecone.app.infra.security.session.cache;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.bluecone.app.infra.security.session.AuthSessionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话热点信息快照，存入 Redis，避免频繁访问数据库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionSnapshot implements Serializable {

    private String sessionId;
    private Long userId;
    private Long tenantId;
    private String clientType;
    private String deviceId;
    private AuthSessionStatus status;
    private LocalDateTime refreshTokenExpireAt;
    private LocalDateTime lastActiveAt;
}
