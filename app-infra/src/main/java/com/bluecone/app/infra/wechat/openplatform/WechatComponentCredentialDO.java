package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信第三方平台凭证实体。
 * <p>
 * 对应表：bc_wechat_component_credential
 * 用于存储第三方平台的 AppID、AppSecret、verify_ticket 和 component_access_token。
 * </p>
 */
@Data
@TableName("bc_wechat_component_credential")
public class WechatComponentCredentialDO implements Serializable {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 第三方平台 AppID
     */
    private String componentAppId;

    /**
     * 第三方平台 AppSecret
     */
    private String componentAppSecret;

    /**
     * 微信推送的 verify_ticket（每 10 分钟推送一次）
     */
    private String componentVerifyTicket;

    /**
     * 第三方平台 access_token
     */
    private String componentAccessToken;

    /**
     * component_access_token 过期时间
     */
    private LocalDateTime componentAccessTokenExpireAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
