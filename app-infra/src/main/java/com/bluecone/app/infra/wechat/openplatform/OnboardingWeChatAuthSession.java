package com.bluecone.app.infra.wechat.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 微信开放平台授权会话。
 * <p>
 * 用于在生成预授权 URL 时绑定租户/门店信息，并在授权回调时验证。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingWeChatAuthSession {

    /**
     * 会话 state（UUID 或雪花 ID）
     */
    private String state;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 门店 ID（可选）
     */
    private Long storeId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 扩展信息（JSON 格式，可选）
     */
    private String extJson;
}

