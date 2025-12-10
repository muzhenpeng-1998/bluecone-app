package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * 查询注册/审核状态返回结果。
 */
@Data
public class WeChatRegisterStatusResult {

    /**
     * 微信返回的小程序 appid（如果已经生成）。
     */
    private String appId;

    /**
     * 当前状态：CREATING/REVIEWING/REJECTED/DONE 等。
     * 这里先使用字符串，后续可在领域层映射为枚举。
     */
    private String status;

    /**
     * 审核或失败原因描述（如果有）。
     */
    private String reason;

    /**
     * 微信返回的原始 JSON。
     */
    private String rawBody;
}

