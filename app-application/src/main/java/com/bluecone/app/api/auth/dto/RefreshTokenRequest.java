package com.bluecone.app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 token 请求。
 */
@Data
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
