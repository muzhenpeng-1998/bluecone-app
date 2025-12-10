package com.bluecone.app.tenant.application.wechat.command;

/**
 * 微信小程序授权/更新授权事件命令。
 * <p>承载微信开放平台 authorized / updateauthorized 事件中的核心字段。</p>
 */
public record WechatAuthorizedEventCommand(
        // 小程序 appid
        String authorizerAppid,
        // 小程序的长期刷新 token（如果事件中能拿到）
        String authorizerRefreshToken,
        // 小程序昵称
        String nickName,
        // 小程序头像 URL
        String headImg,
        // 主体类型：0未知，1企业，2个体工商户，3政府，4媒体，5其他组织
        Integer principalType,
        // 主体名称（公司名/个体户名等）
        String principalName,
        // 功能介绍/简介签名
        String signature,
        // 服务类型/帐号类型（保留微信返回的整数编码）
        Integer serviceType,
        // 认证类型/认证情况（保留微信返回的整数编码）
        Integer verifyType,
        // func_info 原始 JSON
        String funcInfoJson,
        // business_info 原始 JSON，例如支付、卡券等能力开关
        String businessInfoJson,
        // miniprograminfo 原始 JSON，例如类目信息、发布信息等
        String miniprograminfoJson) {
}

