package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信已授权小程序实体。
 * <p>
 * 对应表：bc_wechat_authorized_app
 * 用于存储租户授权给第三方平台的小程序信息。
 * </p>
 */
@Data
@TableName("bc_wechat_authorized_app")
public class WechatAuthorizedAppDO implements Serializable {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 第三方平台 AppID
     */
    private String componentAppId;

    /**
     * 授权方（小程序）AppID
     */
    private String authorizerAppId;

    /**
     * 授权方刷新令牌（用于刷新 authorizer_access_token）
     */
    private String authorizerRefreshToken;

    /**
     * 授权方接口调用令牌
     */
    private String authorizerAccessToken;

    /**
     * authorizer_access_token 过期时间
     */
    private LocalDateTime authorizerAccessTokenExpireAt;

    // ========== 小程序基本信息 ==========

    /**
     * 小程序昵称
     */
    private String nickName;

    /**
     * 小程序头像
     */
    private String headImg;

    /**
     * 小程序类型
     */
    private Integer serviceTypeInfo;

    /**
     * 认证类型
     */
    private Integer verifyTypeInfo;

    /**
     * 原始 ID
     */
    private String userName;

    /**
     * 主体名称
     */
    private String principalName;

    /**
     * 小程序别名
     */
    private String alias;

    /**
     * 二维码图片 URL
     */
    private String qrcodeUrl;

    /**
     * 功能的开通状况（JSON 格式）
     */
    private String businessInfo;

    /**
     * 小程序配置信息（JSON 格式）
     */
    private String miniProgramInfo;

    // ========== 授权状态 ==========

    /**
     * 授权状态：AUTHORIZED=已授权，UNAUTHORIZED=已取消授权
     */
    private String authorizationStatus;

    /**
     * 授权时间
     */
    private LocalDateTime authorizedAt;

    /**
     * 取消授权时间
     */
    private LocalDateTime unauthorizedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

