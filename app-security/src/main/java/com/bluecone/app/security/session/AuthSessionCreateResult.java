package com.bluecone.app.security.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话创建结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionCreateResult {

    private String accessToken;

    private String refreshToken;

    private Long expireAt;

    private Long userId;

    private Long tenantId;
}
