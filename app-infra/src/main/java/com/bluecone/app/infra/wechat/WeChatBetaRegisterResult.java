package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * fastregisterbetaweapp 返回结果简化封装。
 *
 * 实际字段以微信文档为准，这里先保留核心的 appId 和原始 JSON。
 */
@Data
public class WeChatBetaRegisterResult {

    /**
     * 微信返回的小程序 appid（如果立即下发）。
     */
    private String appId;

    /**
     * 微信侧返回的原始 JSON，用于调试和后续扩展。
     */
    private String rawBody;
}

