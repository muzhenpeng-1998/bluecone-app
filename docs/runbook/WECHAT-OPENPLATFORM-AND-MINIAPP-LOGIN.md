# 微信开放平台（第三方平台）+ 小程序登录/注册 联调文档

## 概述

本文档描述如何验证"微信开放平台（第三方平台）+ 小程序登录/注册"功能的完整流程。

## 前置条件

### 1. 微信开放平台配置

在微信开放平台（https://open.weixin.qq.com/）创建第三方平台，并获取以下信息：

- `component_app_id`：第三方平台 appid
- `component_app_secret`：第三方平台 appsecret
- `component_token`：消息校验 Token
- `component_aes_key`：消息加解密 Key

### 2. 环境变量配置

在应用启动前，设置以下环境变量：

```bash
export WECHAT_OPEN_PLATFORM_ENABLED=true
export WECHAT_COMPONENT_APP_ID=wx1234567890abcdef
export WECHAT_COMPONENT_APP_SECRET=your_component_app_secret
export WECHAT_COMPONENT_TOKEN=your_component_token
export WECHAT_COMPONENT_AES_KEY=your_component_aes_key
```

或在 `application-local.yml` 中配置：

```yaml
wechat:
  open-platform:
    enabled: true
    component-app-id: wx1234567890abcdef
    component-app-secret: your_component_app_secret
    component-token: your_component_token
    component-aes-key: your_component_aes_key
```

### 3. 数据库迁移

确保 Flyway 迁移已执行，以下表已创建：

- `bc_user_identity`：用户身份表
- `bc_user_profile`：用户画像表
- `bc_user_external_identity`：用户外部身份绑定表
- `bc_wechat_component_credential`：微信开放平台凭证表
- `bc_wechat_authorized_app`：微信授权小程序表
- `bc_member`：会员表

## 回调 URL 配置

在微信开放平台配置以下回调 URL：

### 1. 授权事件接收 URL

```
https://yourdomain.com/api/wechat/open/callback/ticket
```

用于接收微信开放平台推送的 `component_verify_ticket`。

### 2. 授权完成回调 URL

```
https://yourdomain.com/api/wechat/open/auth/callback
```

用于接收小程序授权完成后的回调。

## 验证步骤

### 步骤 1：验证 Ticket 推送

微信开放平台每 10 分钟推送一次 `component_verify_ticket`。

**验证方法：**

1. 启动应用
2. 在微信开放平台配置授权事件接收 URL
3. 微信会发送 GET 请求验证 URL 有效性
4. 观察应用日志，应该看到类似以下日志：

```
[WechatOpenTicket] GET request for URL verification, msgSignature=xxx, timestamp=xxx, nonce=xxx
[WechatOpenTicket] URL verification successful, returning decrypted echostr
```

5. 等待微信推送 ticket（首次推送可能需要等待 10 分钟）
6. 观察应用日志，应该看到类似以下日志：

```
[WechatOpenTicket] POST request received, msgSignature=xxx, timestamp=xxx, nonce=xxx
[WechatOpenTicket] InfoType: component_verify_ticket
[WechatOpenTicket] ComponentVerifyTicket saved successfully: ticket@@@...
```

7. 查询数据库，确认 `bc_wechat_component_credential` 表中有 `verify_ticket` 记录：

```sql
SELECT * FROM bc_wechat_component_credential WHERE component_app_id = 'wx1234567890abcdef';
```

### 步骤 2：小程序授权

**前提：** 已收到至少一次 `component_verify_ticket`。

**验证方法：**

1. 调用租户授权接口获取授权 URL：

```bash
curl -X POST http://localhost/api/tenant/wechat/authorize/url \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "tenantId": 1001
  }'
```

响应示例：

```json
{
  "authUrl": "https://mp.weixin.qq.com/cgi-bin/componentloginpage?component_appid=wx1234567890abcdef&pre_auth_code=xxx&redirect_uri=https://yourdomain.com/api/wechat/open/auth/callback&state=session_token_xxx"
}
```

2. 在浏览器中打开 `authUrl`，使用小程序管理员账号扫码授权
3. 授权完成后，浏览器会重定向到回调 URL
4. 观察应用日志，应该看到类似以下日志：

```
[WechatOpenAuth] auth callback received, authCode=xxx, expiresIn=xxx, sessionToken=xxx
[WechatOpenAuth] queryAuth success, authorizerAppId=wx9876543210fedcba
[WechatOpenAuth] getAuthorizerInfo success, nickName=测试小程序
```

5. 查询数据库，确认 `bc_wechat_authorized_app` 表中有授权记录：

```sql
SELECT * FROM bc_wechat_authorized_app WHERE authorizer_app_id = 'wx9876543210fedcba';
```

确认以下字段：

- `tenant_id`：正确的租户 ID
- `authorization_status`：`AUTHORIZED`
- `authorizer_refresh_token`：不为空
- `nick_name`：小程序昵称

### 步骤 3：小程序登录/注册

**前提：** 小程序已授权给第三方平台。

**验证方法：**

#### 3.1 小程序端获取 code 和 appId

在小程序端调用以下代码：

```javascript
// 获取小程序 appId
const accountInfo = wx.getAccountInfoSync();
const authorizerAppId = accountInfo.miniProgram.appId;

// 获取登录 code
wx.login({
  success: (res) => {
    const code = res.code;
    
    // 调用后端登录接口
    wx.request({
      url: 'https://yourdomain.com/api/user/auth/wechat-miniapp/login',
      method: 'POST',
      data: {
        code: code,
        authorizerAppId: authorizerAppId
      },
      success: (loginRes) => {
        console.log('登录成功', loginRes.data);
        // 保存 accessToken
        wx.setStorageSync('accessToken', loginRes.data.accessToken);
      }
    });
  }
});
```

#### 3.2 后端登录接口

```bash
curl -X POST http://localhost/api/user/auth/wechat-miniapp/login \
  -H "Content-Type: application/json" \
  -d '{
    "code": "081234567890abcdef",
    "authorizerAppId": "wx9876543210fedcba"
  }'
```

响应示例：

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expireAt": "2025-12-23T12:00:00Z",
  "userId": 1001,
  "tenantId": 1001,
  "memberId": 2001,
  "newUser": true,
  "newMember": true
}
```

#### 3.3 验证登录结果

1. 观察应用日志，应该看到类似以下日志：

```
[UserAuth] WeChat mini app login, appId=wx9876543210fedcba, tenantId=1001
[UserAuth] code2Session success, openId前3后3=abc***xyz, unionId=有值
[UserAuth] 创建新用户: userId=1001, unionId=***, phone=null, appId=wx9876543210fedcba
[UserAuth] 绑定外部身份成功: provider=WECHAT_MINI, appId=wx9876543210fedcba, openId前3后3=abc***xyz, userId=1001
[UserAuth] Login success, userId=1001, tenantId=1001, memberId=2001, newUser=true, newMember=true
```

2. 查询数据库，确认用户已创建：

```sql
-- 查询用户身份
SELECT * FROM bc_user_identity WHERE id = 1001;

-- 查询用户画像
SELECT * FROM bc_user_profile WHERE user_id = 1001;

-- 查询外部身份绑定
SELECT * FROM bc_user_external_identity WHERE user_id = 1001;

-- 查询会员
SELECT * FROM bc_member WHERE user_id = 1001 AND tenant_id = 1001;
```

3. 使用返回的 `accessToken` 调用需要登录的接口，验证 token 有效：

```bash
curl -X GET http://localhost/api/user/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 步骤 4：验证 unionId 为空的场景

**场景：** 用户未绑定开放平台，unionId 为空。

**验证方法：**

1. 使用一个未绑定开放平台的小程序进行登录
2. 观察应用日志，应该看到类似以下日志：

```
[UserAuth] code2Session success, openId前3后3=abc***xyz, unionId=空
[UserAuth] 通过 external_identity (appId, openId) 找到用户: userId=1001
```

或者（如果是新用户）：

```
[UserAuth] 创建新用户: userId=1002, unionId=null, phone=null, appId=wx9876543210fedcba
[UserAuth] 绑定外部身份成功: provider=WECHAT_MINI, appId=wx9876543210fedcba, openId前3后3=abc***xyz, userId=1002
```

3. 查询数据库，确认 `bc_user_external_identity` 表中有绑定记录：

```sql
SELECT * FROM bc_user_external_identity 
WHERE provider = 'WECHAT_MINI' 
  AND app_id = 'wx9876543210fedcba' 
  AND open_id = 'oABC123456789XYZ';
```

### 步骤 5：验证取消授权事件

**场景：** 小程序管理员在微信公众平台取消授权。

**验证方法：**

1. 在微信公众平台取消小程序授权
2. 微信会推送 `unauthorized` 事件到回调 URL
3. 观察应用日志，应该看到类似以下日志：

```
[WechatOpenTicket] POST request received, msgSignature=xxx, timestamp=xxx, nonce=xxx
[WechatOpenTicket] InfoType: unauthorized
[WechatOpenTicket] Received unauthorized event, authorizerAppId=wx9876543210fedcba
```

4. 查询数据库，确认 `bc_wechat_authorized_app` 表中的授权状态已更新：

```sql
SELECT authorization_status, unauthorized_at 
FROM bc_wechat_authorized_app 
WHERE authorizer_app_id = 'wx9876543210fedcba';
```

应该看到：

- `authorization_status`：`UNAUTHORIZED`
- `unauthorized_at`：取消授权时间

## 常见问题排查

### 1. 未收到 component_verify_ticket

**可能原因：**

- 回调 URL 配置错误
- 回调 URL 无法访问（防火墙/网络问题）
- 消息加解密配置错误

**排查方法：**

1. 检查微信开放平台配置的回调 URL 是否正确
2. 使用 `curl` 测试回调 URL 是否可访问：

```bash
curl -X GET 'http://yourdomain.com/api/wechat/open/callback/ticket?msg_signature=xxx&timestamp=xxx&nonce=xxx&echostr=xxx'
```

3. 检查应用日志，查看是否有错误信息
4. 检查 `component_token` 和 `component_aes_key` 配置是否正确

### 2. 小程序登录失败：小程序未接入或未授权

**错误信息：**

```
小程序未接入或未授权，appId=wx9876543210fedcba
```

**可能原因：**

- 小程序未授权给第三方平台
- 授权记录未写入数据库
- `authorizerAppId` 传错

**排查方法：**

1. 查询数据库，确认授权记录是否存在：

```sql
SELECT * FROM bc_wechat_authorized_app WHERE authorizer_app_id = 'wx9876543210fedcba';
```

2. 如果不存在，重新执行小程序授权流程
3. 检查小程序端传的 `authorizerAppId` 是否正确

### 3. code2Session 失败

**错误信息：**

```
code2Session 失败: errcode=40029, errmsg=invalid code
```

**可能原因：**

- code 已过期（5 分钟有效期）
- code 已使用过（一次性）
- code 不属于该小程序

**排查方法：**

1. 确认 code 是刚获取的（5 分钟内）
2. 确认 code 未被使用过
3. 确认 `authorizerAppId` 与 code 对应的小程序一致

### 4. 获取手机号失败

**错误信息：**

```
获取手机号失败: errcode=40001, errmsg=invalid credential
```

**可能原因：**

- `authorizer_access_token` 过期或无效
- 小程序未开通获取手机号权限

**排查方法：**

1. 查询数据库，确认 `authorizer_access_token` 是否有效：

```sql
SELECT authorizer_access_token, authorizer_access_token_expire_at 
FROM bc_wechat_authorized_app 
WHERE authorizer_app_id = 'wx9876543210fedcba';
```

2. 如果过期，等待自动刷新或手动触发刷新
3. 确认小程序已开通获取手机号权限

## 安全注意事项

1. **不要在日志中打印敏感信息**：
   - openId、unionId、手机号等应该脱敏（只显示前3后3）
   - access_token、refresh_token 不要打印明文

2. **验证微信签名**：
   - 所有微信回调接口必须验证签名，防止伪造请求
   - 使用 `msg_signature` 而不是 `signature`

3. **AES 解密**：
   - 微信推送的消息都是加密的，必须解密后才能使用
   - 使用 WxJava 的 `WxCryptUtil` 工具类进行解密

4. **不信任客户端传的 tenantId**：
   - 以 `authorizerAppId` 为主键，从数据库反查 `tenantId`
   - 防止客户端伪造 `tenantId` 访问其他租户数据

5. **限制 permitAll 路径**：
   - 只放行必要的公开接口
   - `/api/admin/**` 不应该 permitAll

## 性能优化建议

1. **缓存 component_access_token**：
   - `component_access_token` 有效期 2 小时，应该缓存
   - 使用 Redis 或内存缓存

2. **缓存 authorizer_access_token**：
   - `authorizer_access_token` 有效期 2 小时，应该缓存
   - 在过期前 2 分钟自动刷新

3. **异步处理授权事件**：
   - 授权回调可以先返回成功，再异步处理授权信息
   - 使用消息队列或事件总线

4. **数据库索引**：
   - `bc_wechat_authorized_app.authorizer_app_id` 应该有索引
   - `bc_user_external_identity(provider, app_id, open_id)` 应该有唯一索引

## 总结

完成以上验证步骤后，应该确认以下功能正常：

- ✅ 微信开放平台 ticket 推送正常接收
- ✅ 小程序授权流程正常
- ✅ 小程序登录/注册正常
- ✅ unionId 为空时使用 external_identity 兜底
- ✅ 取消授权事件正常处理
- ✅ 安全配置正确（不泛开放行）

如有问题，请查看应用日志和数据库记录进行排查。

