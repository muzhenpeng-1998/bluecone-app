package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 微信开放平台组件凭证存储表映射，表名：bc_wechat_component_credential。
 * 存储 component_verify_ticket 与 component_access_token 等敏感信息。
 */
@Data
@TableName("bc_wechat_component_credential")
public class WechatComponentCredentialDO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 第三方平台组件 appid
     */
    private String componentAppId;

    /**
     * 最近一次收到的 component_verify_ticket（明文或加密后存储）
     */
    private String componentVerifyTicket;

    /**
     * 当前可用的 component_access_token（明文或加密）
     */
    private String componentAccessToken;

    /**
     * 当前 access_token 预计过期时间（服务端本地时间）
     */
    private LocalDateTime accessTokenExpiresAt;

    /**
     * 最近一次更新 component_verify_ticket 的时间
     */
    private LocalDateTime lastTicketAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

