package com.bluecone.app.user.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private Long tenantId;

    private Boolean newUser;

    private Boolean newMember;

    private Long memberId;

    private Long expireAt;
}
