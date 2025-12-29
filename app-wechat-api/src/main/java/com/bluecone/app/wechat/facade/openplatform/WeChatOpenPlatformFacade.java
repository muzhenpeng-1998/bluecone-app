package com.bluecone.app.wechat.facade.openplatform;

/**
 * 微信开放平台 Facade 接口。
 * <p>
 * 对外暴露预授权链接生成、授权回调处理等功能，隐藏 WxJava SDK 实现细节。
 * </p>
 */
public interface WeChatOpenPlatformFacade {

    /**
     * 生成预授权链接。
     * <p>
     * 内部流程：
     * 1. 获取或刷新 component_access_token
     * 2. 创建 pre_auth_code
     * 3. 拼接授权链接（包含 redirect_uri 和自定义参数）
     * </p>
     *
     * @param command 预授权链接生成命令（包含 tenantId/storeId/redirectUri 等）
     * @return 预授权链接结果
     */
    WeChatPreAuthUrlResult buildPreAuthUrl(WeChatPreAuthUrlCommand command);

    /**
     * 处理微信开放平台回调（统一入口）。
     * <p>
     * 支持的 InfoType：
     * - component_verify_ticket：保存 ticket
     * - authorized：小程序授权
     * - updateauthorized：小程序更新授权
     * - unauthorized：小程序取消授权
     * </p>
     * <p>
     * 内部流程：
     * 1. 验签 + 解密
     * 2. 解析 InfoType
     * 3. 幂等检查（InfoType + createTime + appid + bodyHash）
     * 4. 根据 InfoType 分发处理：
     *    - ticket：写入 component_credential 表
     *    - authorized/updateauthorized：queryAuth + 落库
     *    - unauthorized：标记解绑
     * 5. 返回 success/fail
     * </p>
     *
     * @param command 回调处理命令（包含原始请求参数和 body）
     * @return 回调处理结果（success/fail）
     */
    WeChatOpenCallbackResult handleCallback(WeChatOpenCallbackCommand command);

    /**
     * 使用授权码查询授权信息（用于浏览器授权回调）。
     * <p>
     * 内部流程：
     * 1. 使用 auth_code 调用 queryAuth 获取 authorizer_access_token/refresh_token
     * 2. 调用 getAuthorizerInfo 获取小程序基本信息
     * 3. 保存到 bc_wechat_authorized_app 表
     * 4. 返回授权方信息
     * </p>
     * <p>
     * 注意：此方法不涉及租户绑定，租户绑定由 app-tenant 完成。
     * </p>
     *
     * @param command queryAuth 命令（包含 auth_code）
     * @return queryAuth 结果（包含 authorizerAppId/nickName 等）
     */
    WeChatQueryAuthResult queryAuth(WeChatQueryAuthCommand command);
}


