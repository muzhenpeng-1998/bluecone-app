# 微信开放平台集成指南

## 概述

本文档说明如何配置和使用微信开放平台（第三方平台）和小程序登录功能。

## 一、配置说明

### 1.1 application.yml 配置

在 `application.yml` 或 `application-{profile}.yml` 中添加以下配置：

```yaml
# 微信开放平台配置
wechat:
  open-platform:
    # 是否启用微信开放平台（true=使用 WxJava 实现，false=使用 Stub 实现）
    enabled: ${WECHAT_OPEN_PLATFORM_ENABLED:false}
    
    # 第三方平台 AppID（从微信开放平台获取）
    component-app-id: ${WECHAT_COMPONENT_APP_ID:}
    
    # 第三方平台 AppSecret（从微信开放平台获取）
    component-app-secret: ${WECHAT_COMPONENT_APP_SECRET:}
    
    # 第三方平台 Token（用于消息签名校验）
    component-token: ${WECHAT_COMPONENT_TOKEN:}
    
    # 第三方平台 AES Key（用于消息加密解密）
    component-aes-key: ${WECHAT_COMPONENT_AES_KEY:}
```

### 1.2 环境变量配置

生产环境建议通过环境变量注入敏感配置：

```bash
# 启用微信开放平台
export WECHAT_OPEN_PLATFORM_ENABLED=true

# 第三方平台凭证（从微信开放平台获取）
export WECHAT_COMPONENT_APP_ID=wx1234567890abcdef
export WECHAT_COMPONENT_APP_SECRET=your_component_app_secret_here
export WECHAT_COMPONENT_TOKEN=your_component_token_here
export WECHAT_COMPONENT_AES_KEY=your_component_aes_key_here
```

### 1.3 安全白名单配置

以下路径需要配置为 permitAll（不需要登录认证）：

```yaml
bluecone:
  user:
    context:
      # 允许匿名访问的路径
      allow-anonymous-paths:
        - /api/user/auth/**              # 用户登录接口
        - /api/wechat/open/callback/**   # 微信开放平台回调接口（新增）
```

**重要说明：**
- `/api/wechat/open/callback/**` 必须 permitAll，因为微信服务器回调时不会携带认证信息
- 回调接口内部会进行微信签名校验，确保消息来自微信服务器
- **不要**将 `/api/admin/**` 配置为 permitAll，管理后台接口必须鉴权

## 二、数据库表结构

### 2.1 bc_wechat_component_credential

存储第三方平台凭证（component_verify_ticket 和 component_access_token）。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| component_app_id | VARCHAR(64) | 第三方平台 AppID |
| component_app_secret | VARCHAR(128) | 第三方平台 AppSecret |
| component_verify_ticket | VARCHAR(512) | 微信推送的 verify_ticket |
| component_access_token | VARCHAR(512) | 第三方平台 access_token |
| component_access_token_expire_at | DATETIME | token 过期时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 2.2 bc_wechat_authorized_app

存储租户授权的小程序信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| tenant_id | BIGINT | 租户 ID |
| component_app_id | VARCHAR(64) | 第三方平台 AppID |
| authorizer_app_id | VARCHAR(64) | 授权方（小程序）AppID |
| authorizer_refresh_token | VARCHAR(512) | 授权方刷新令牌 |
| authorizer_access_token | VARCHAR(512) | 授权方接口调用令牌 |
| authorizer_access_token_expire_at | DATETIME | token 过期时间 |
| nick_name | VARCHAR(128) | 小程序昵称 |
| head_img | VARCHAR(512) | 小程序头像 |
| authorization_status | VARCHAR(32) | 授权状态（AUTHORIZED/UNAUTHORIZED） |
| authorized_at | DATETIME | 授权时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

## 三、接口说明

### 3.1 微信开放平台回调接口

#### 接收 component_verify_ticket

**接口地址：** `GET/POST /api/wechat/open/callback/ticket`

**说明：**
- 微信开放平台每 10 分钟推送一次 component_verify_ticket
- 第三方平台需要接收并保存，用于后续获取 component_access_token
- 首次配置时，微信会发送 GET 请求验证 URL 有效性

**请求参数：**
- `signature`: 微信加密签名
- `timestamp`: 时间戳
- `nonce`: 随机数
- `echostr`: 随机字符串（仅 GET 请求）
- `encrypt_type`: 加密类型（aes 表示加密消息）
- `msg_signature`: 消息签名（加密消息时使用）

**返回值：**
- GET 请求：返回 `echostr` 原值
- POST 请求：返回 `success`

### 3.2 小程序登录接口

#### 微信小程序登录

**接口地址：** `POST /api/user/auth/wechat-miniapp/login`

**请求体：**
```json
{
  "code": "wx_login_code",           // 必填，wx.login 获取的 code
  "phoneCode": "phone_code",         // 可选，推荐方式，wx.getPhoneNumber 获取的 code
  "encryptedData": "encrypted_data", // 可选，兼容旧版本，加密的手机号数据
  "iv": "iv_string",                 // 可选，兼容旧版本，初始向量
  "sourceTenantId": 1001,            // 必填，租户 ID
  "sourceChannel": "wechat_mini"     // 可选，投放渠道标识
}
```

**返回值：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expireAt": 1703001234,
    "userId": 10001,
    "tenantId": 1001,
    "memberId": 20001,
    "newUser": true,    // 是否新注册用户
    "newMember": true   // 是否新会员
  }
}
```

**说明：**
1. 后端会根据 `sourceTenantId` 查询该租户授权的小程序 AppID
2. 使用 `code` 换取 openId、unionId、sessionKey
3. 如果提供了 `phoneCode`，会调用微信接口获取手机号（推荐）
4. 如果提供了 `encryptedData` 和 `iv`，会解密获取手机号（兼容旧版本）
5. 根据 unionId 或 (appId, openId) 注册或加载用户
6. 创建登录会话，返回 access_token 和 refresh_token

## 四、使用流程

### 4.1 第三方平台授权流程

1. **配置回调 URL**
   - 在微信开放平台配置授权事件接收 URL：`https://yourdomain.com/api/wechat/open/callback/ticket`
   - 微信会发送 GET 请求验证 URL 有效性

2. **接收 component_verify_ticket**
   - 微信每 10 分钟推送一次 component_verify_ticket
   - 后端接收并保存到 `bc_wechat_component_credential` 表

3. **生成授权页 URL**
   - 调用 `WeChatOpenPlatformClient.createPreAuthCode()` 获取预授权码
   - 拼接授权页 URL：`https://mp.weixin.qq.com/cgi-bin/componentloginpage?component_appid=XXX&pre_auth_code=XXX&redirect_uri=XXX`

4. **商户扫码授权**
   - 商户扫码后，微信会重定向到 `redirect_uri`，并带上 `auth_code`

5. **获取授权信息**
   - 使用 `auth_code` 调用 `queryAuth()` 获取授权信息
   - 调用 `getAuthorizerInfo()` 获取小程序基本信息
   - 保存到 `bc_wechat_authorized_app` 表

### 4.2 小程序登录流程

1. **小程序端调用 wx.login**
   ```javascript
   wx.login({
     success: (res) => {
       const code = res.code;
       // 将 code 发送到后端
     }
   });
   ```

2. **小程序端获取手机号（推荐方式）**
   ```javascript
   // 方式一：使用 wx.getPhoneNumber（推荐）
   wx.getPhoneNumber({
     success: (res) => {
       const phoneCode = res.code;
       // 将 phoneCode 和 loginCode 一起发送到后端
     }
   });
   
   // 方式二：使用 button open-type="getPhoneNumber"（兼容旧版本）
   <button open-type="getPhoneNumber" bindgetphonenumber="getPhoneNumber">
     获取手机号
   </button>
   ```

3. **发送登录请求到后端**
   ```javascript
   wx.request({
     url: 'https://yourdomain.com/api/user/auth/wechat-miniapp/login',
     method: 'POST',
     data: {
       code: loginCode,
       phoneCode: phoneCode,  // 推荐方式
       sourceTenantId: 1001
     },
     success: (res) => {
       // 保存 accessToken 和 refreshToken
       wx.setStorageSync('accessToken', res.data.data.accessToken);
       wx.setStorageSync('refreshToken', res.data.data.refreshToken);
     }
   });
   ```

4. **后端处理登录**
   - 根据 `sourceTenantId` 查询授权的小程序 AppID
   - 使用 `code` 换取 openId、unionId、sessionKey
   - 解密手机号（如果提供）
   - 注册或加载用户
   - 创建登录会话
   - 返回 token

## 五、注意事项

### 5.1 安全注意事项

1. **敏感配置保护**
   - `component-app-secret`、`component-token`、`component-aes-key` 必须通过环境变量注入
   - 不要将敏感配置提交到代码仓库

2. **回调接口安全**
   - 微信回调接口必须进行签名校验，防止伪造请求
   - 不要将回调接口暴露给公网（除了微信服务器）

3. **Token 安全**
   - `component_access_token` 和 `authorizer_access_token` 有过期时间，需要定期刷新
   - 不要将 token 暴露给客户端

### 5.2 开发调试

1. **使用 Stub 实现**
   - 开发环境可以设置 `wechat.open-platform.enabled=false` 使用 Stub 实现
   - Stub 实现会返回模拟数据，方便本地调试

2. **日志查看**
   - 所有微信相关操作都有详细日志，可以通过日志排查问题
   - 日志关键字：`[WxJavaOpenPlatform]`、`[WxJavaMiniApp]`、`[WechatOpenTicket]`

3. **测试环境配置**
   - 测试环境需要配置真实的微信开放平台凭证
   - 需要在微信开放平台配置测试环境的回调 URL

### 5.3 生产部署

1. **环境变量配置**
   - 确保所有环境变量都已正确配置
   - 使用 K8s Secret 或其他密钥管理工具管理敏感配置

2. **数据库迁移**
   - 确保 Flyway migration 已执行，数据库表已创建
   - 检查表结构和索引是否正确

3. **回调 URL 配置**
   - 在微信开放平台配置生产环境的回调 URL
   - 确保回调 URL 可以被微信服务器访问（公网可达）

4. **监控告警**
   - 监控 `component_verify_ticket` 的接收情况（应该每 10 分钟收到一次）
   - 监控 token 刷新失败的情况
   - 监控登录失败率

## 六、常见问题

### Q1: component_verify_ticket 一直收不到？

**A:** 检查以下几点：
1. 回调 URL 是否配置正确
2. 回调 URL 是否可以被微信服务器访问（公网可达）
3. 回调接口是否返回正确的响应（GET 请求返回 echostr，POST 请求返回 success）
4. 检查日志，是否有签名校验失败的错误

### Q2: 小程序登录失败，提示"租户未授权小程序"？

**A:** 检查以下几点：
1. 租户是否已完成小程序授权流程
2. `bc_wechat_authorized_app` 表中是否有该租户的授权记录
3. 授权状态是否为 `AUTHORIZED`

### Q3: 手机号获取失败？

**A:** 检查以下几点：
1. 小程序是否已开通手机号获取权限
2. 使用 `phoneCode` 方式（推荐）还是 `encryptedData/iv` 方式（兼容）
3. `sessionKey` 是否有效（`sessionKey` 有过期时间）
4. 检查日志，查看具体错误信息

### Q4: 如何在本地开发环境调试？

**A:** 本地开发环境建议：
1. 设置 `wechat.open-platform.enabled=false` 使用 Stub 实现
2. 或者使用内网穿透工具（如 ngrok）将本地服务暴露到公网
3. 在微信开放平台配置内网穿透的回调 URL

## 七、参考资料

- [微信开放平台官方文档](https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/2.0/getting_started/how_to_read.html)
- [WxJava 官方文档](https://github.com/Wechat-Group/WxJava)
- [微信小程序登录文档](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)

