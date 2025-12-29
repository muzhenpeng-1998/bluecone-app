package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * queryAuth 结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatQueryAuthResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 授权方 AppID
     */
    private String authorizerAppId;

    /**
     * 小程序昵称
     */
    private String nickName;

    /**
     * 小程序头像 URL
     */
    private String headImg;

    /**
     * 主体名称
     */
    private String principalName;

    /**
     * 错误码（如果失败）
     */
    private Integer errcode;

    /**
     * 错误信息（如果失败）
     */
    private String errmsg;
}

