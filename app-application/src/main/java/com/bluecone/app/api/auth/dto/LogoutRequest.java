package com.bluecone.app.api.auth.dto;

import lombok.Data;

/**
 * 登出请求。
 */
@Data
public class LogoutRequest {

    private String sessionId;
}
