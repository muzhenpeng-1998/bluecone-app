package com.bluecone.app.api.auth.dto;

import lombok.Data;

/**
 * 踢出所有设备请求。
 */
@Data
public class LogoutAllRequest {

    private String clientType;
}
