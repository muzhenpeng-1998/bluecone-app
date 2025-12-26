package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信开放平台回调处理命令。
 * <p>
 * 包含微信回调的所有原始参数和请求体。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatOpenCallbackCommand {

    /**
     * 微信签名（URL 参数 signature）
     */
    private String signature;

    /**
     * 时间戳（URL 参数 timestamp）
     */
    private String timestamp;

    /**
     * 随机串（URL 参数 nonce）
     */
    private String nonce;

    /**
     * 加密消息签名（URL 参数 msg_signature）
     */
    private String msgSignature;

    /**
     * 加密类型（URL 参数 encrypt_type，如 aes）
     */
    private String encryptType;

    /**
     * 原始请求体（加密 XML）
     */
    private String rawBody;

    /**
     * 租户 ID（可选，用于多租户场景的路由）
     */
    private Long tenantId;

    /**
     * 门店 ID（可选）
     */
    private Long storeId;
}

