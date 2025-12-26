package com.bluecone.app.infra.wechat.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 微信开放平台已授权小程序账户表映射，表名：bc_wechat_authorized_app。
 */
@Data
@TableName("bc_wechat_authorized_app")
public class WechatAuthorizedAppDO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属租户 ID，对应 bc_tenant.id
     */
    private Long tenantId;

    /**
     * 第三方平台 AppID
     */
    private String componentAppId;

    /**
     * 授权方小程序 appid，全局唯一
     */
    private String authorizerAppId;

    /**
     * 授权方刷新令牌（建议应用层加密后再入库）
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

    /**
     * 小程序昵称
     */
    private String nickName;

    /**
     * 小程序头像 URL
     */
    private String headImg;

    /**
     * 小程序类型（service_type_info）
     */
    private Integer serviceTypeInfo;

    /**
     * 认证类型（verify_type_info）
     */
    private Integer verifyTypeInfo;

    /**
     * 原始 ID（user_name）
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

