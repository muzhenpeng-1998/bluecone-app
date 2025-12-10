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
     * 所属门店 ID，对应 bc_store.id，可为空（租户级小程序时为 NULL）
     */
    private Long storeId;

    /**
     * 授权方小程序 appid，全局唯一
     */
    private String authorizerAppid;

    /**
     * 授权方刷新令牌（建议应用层加密后再入库）
     */
    private String authorizerRefreshToken;

    /**
     * 小程序名称（微信昵称）
     */
    private String nickName;

    /**
     * 小程序头像 URL
     */
    private String headImg;

    /**
     * 主体类型：0-未知，1-企业，2-个体工商户，3-政府，4-媒体，5-其他组织
     */
    private Integer principalType;

    /**
     * 主体名称（公司名/个体户名等）
     */
    private String principalName;

    /**
     * 功能介绍/简介签名
     */
    private String signature;

    /**
     * 服务类型/帐号类型（保留微信返回的整数编码）
     */
    private Integer serviceType;

    /**
     * 认证类型/认证情况（保留微信返回的整数编码）
     */
    private Integer verifyType;

    /**
     * 授权给第三方的权限集列表原始 JSON 快照
     */
    private String funcInfoJson;

    /**
     * business_info 原始 JSON，例如支付、卡券等能力开关
     */
    private String businessInfoJson;

    /**
     * 小程序相关配置原始 JSON，例如类目信息、发布信息等
     */
    private String miniprograminfoJson;

    /**
     * 授权状态：1-已授权，2-已取消，3-已封禁/失效
     */
    private Integer authStatus;

    /**
     * 认证状态：0-未认证，1-已认证，2-认证失败，3-认证过期
     */
    private Integer certStatus;

    /**
     * 首次授权时间
     */
    private LocalDateTime firstAuthTime;

    /**
     * 最近一次授权信息同步时间
     */
    private LocalDateTime lastAuthUpdateAt;

    /**
     * 取消授权时间
     */
    private LocalDateTime canceledAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

