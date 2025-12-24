# 微信开放平台代运营多小程序实现说明

## 概述

本次实现打通了「微信开放平台代运营多个商户小程序」的关键链路，包括：

1. ✅ 小程序登录 (code2Session)
2. ✅ 获取手机号 (getPhoneNumberByCode / decryptPhoneNumber)
3. ✅ 授权换 token (queryAuth / refreshAuthorizerToken)
4. ✅ 预授权码 (createPreAuthCode)
5. ✅ 获取组件凭证 (getComponentAccessToken)
6. ✅ 获取授权方信息 (getAuthorizerInfo)

## 实现原则

- **Token 权威来源是 DB**：所有 token（component_access_token、authorizer_access_token）由 `WechatComponentCredentialService` 和 `WechatAuthorizedAppService` 管理，存储在数据库中
- **不依赖 WxJava 内存 configStorage**：调用微信接口时，通过 WxJava 的 `get/post` 方法手动携带 token
- **保持业务层不变**：`app-member` 的 `UserAuthApplicationService` 无需修改，仅依赖 `app-infra` 的接口
- **完整的异常处理**：所有微信接口错误都会打印 errcode/errmsg，并转换为 `IllegalStateException`
- **详细的日志记录**：关键操作都有日志输出，敏感信息（appId、token、手机号）已脱敏

## 文件变更清单

### 1. app-wechat/pom.xml
**变更**：新增 `weixin-java-miniapp` 依赖
```xml
<dependency>
    <groupId>com.github.binarywang</groupId>
    <artifactId>weixin-java-miniapp</artifactId>
    <version>${wx-java.version}</version>
</dependency>
```

### 2. app-infra/src/main/java/com/bluecone/app/infra/wechat/openplatform/AuthorizationInfo.java
**变更**：新增类（补齐缺失的 DTO）

**说明**：对应微信 `query_auth` 接口返回的 `authorization_info` 部分，包含：
- `authorizerAppid`：授权方 appid
- `authorizerAccessToken`：授权方接口调用令牌
- `expiresIn`：有效期（秒）
- `authorizerRefreshToken`：授权方刷新令牌
- `funcInfo`：授权权限集列表

### 3. app-infra/src/main/java/com/bluecone/app/infra/wechat/openplatform/QueryAuthResult.java
**变更**：新增 `authorizationInfo` 字段

**说明**：补充完整的授权信息对象，方便上层业务使用

### 4. app-wechat/src/main/java/com/bluecone/app/wechat/openplatform/WxJavaWeChatOpenPlatformClient.java
**变更**：完整实现所有开放平台接口

**实现方法**：
- `getComponentAccessToken()`：获取组件凭证
  - 调用 `API_COMPONENT_TOKEN_URL`
  - POST body: `{component_appid, component_appsecret, component_verify_ticket}`
  - 解析返回的 `component_access_token` 和 `expires_in`

- `createPreAuthCode()`：创建预授权码
  - 调用 `API_CREATE_PREAUTHCODE_URL`
  - POST body: `{component_appid}`
  - 携带 `component_access_token` 作为 query 参数
  - 解析返回的 `pre_auth_code` 和 `expires_in`

- `queryAuth()`：使用授权码查询授权信息
  - 调用 `API_QUERY_AUTH_URL`
  - POST body: `{component_appid, authorization_code}`
  - 携带 `component_access_token` 作为 query 参数
  - 解析返回的 `authorization_info`（包含 authorizer_appid、authorizer_access_token、authorizer_refresh_token、func_info）

- `refreshAuthorizerToken()`：刷新授权方令牌
  - 调用 `API_AUTHORIZER_TOKEN_URL`
  - POST body: `{component_appid, authorizer_appid, authorizer_refresh_token}`
  - 携带 `component_access_token` 作为 query 参数
  - 解析返回的 `authorizer_access_token`、`authorizer_refresh_token`（可能更新）、`expires_in`
  - **注意**：微信可能返回新的 `refresh_token`，必须覆盖旧的

- `getAuthorizerInfo()`：获取授权方基本信息
  - 调用 `API_GET_AUTHORIZER_INFO_URL`
  - POST body: `{component_appid, authorizer_appid}`
  - 携带 `component_access_token` 作为 query 参数
  - 解析返回的 `authorizer_info`（nick_name、principal_name、head_img、signature、verify_type_info、service_type_info）

### 5. app-wechat/src/main/java/com/bluecone/app/wechat/miniapp/WxJavaWeChatMiniAppClient.java
**变更**：完整实现小程序登录和获取手机号

**实现方法**：
- `code2Session()`：小程序登录（第三方平台模式）
  - 从 `WechatComponentCredentialService` 获取有效的 `component_access_token`
  - 调用 `MINIAPP_JSCODE_2_SESSION` 接口（GET 请求）
  - URL 参数：`appid`、`js_code`、`grant_type=authorization_code`、`component_appid`、`component_access_token`
  - 解析返回的 `openid`、`session_key`、`unionid`（可选）
  - **注意**：第三方平台模式下，必须使用 `component_access_token`，不能使用普通的 `access_token`

- `getPhoneNumberByCode()`：通过 phoneCode 获取手机号（新版接口，推荐）
  - 从 `WechatAuthorizedAppService` 获取或刷新 `authorizer_access_token`
  - 通过 `WxOpenService.getWxOpenComponentService().getWxMaServiceByAppid()` 获取 `WxMaService`
  - 强制设置 `WxMaService.getWxMaConfig().setAccessToken()`（避免 WxMaService 自己去刷新 token）
  - 调用 `WxMaService.getUserService().getPhoneNoInfo(phoneCode)`
  - 解析返回的 `phoneNumber` 和 `countryCode`

- `decryptPhoneNumber()`：解密手机号（旧版接口，兼容）
  - 使用 `WxMaCryptUtils.decrypt(sessionKey, encryptedData, iv)` 进行 AES 解密
  - 解析 JSON 获取 `phoneNumber` 和 `countryCode`
  - **注意**：新版推荐使用 `getPhoneNumberByCode`，此方法仅用于兼容旧版本

## 配置要求

确保 `application.yml` 中配置了以下项：

```yaml
wechat:
  open-platform:
    enabled: true
    component-app-id: wx1234567890abcdef  # 第三方平台 appid
    component-app-secret: your_secret      # 第三方平台 appsecret
    component-token: your_token            # 消息校验 Token
    component-aes-key: your_aes_key        # 消息加解密 Key
  wxjava:
    enabled: true
```

## 关键日志示例

### 1. 刷新 component_access_token
```
[WechatComponentCredential] refreshed component_access_token for componentAppId=wx1234***, expiresIn=7200s
```

### 2. 刷新 authorizer_access_token
```
[WechatAuthorizedAppService] 刷新 authorizer_access_token, appId=wx5678***
[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 成功, authorizerAppId=wx5678***, expiresIn=7200s
[WechatAuthorizedAppService] 更新 authorizer_access_token 成功, appId=wx5678***, expireAt=2025-12-24T14:30:00
```

### 3. 小程序登录
```
[WxJavaWeChatMiniAppClient] code2Session, appId=wx5678****, code=081XYZ...
[WxJavaWeChatMiniAppClient] code2Session 成功, appId=wx5678****, openId=oABC1234...
```

### 4. 获取手机号
```
[WxJavaWeChatMiniAppClient] getPhoneNumberByCode, authorizerAppId=wx5678****, phoneCode=abc123...
[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 成功, authorizerAppId=wx5678****, phone=138****5678
```

## 最小联调示例

### 小程序登录接口

```bash
curl -X POST http://localhost:8080/api/open/user/auth/wechat/miniapp/login \
  -H "Content-Type: application/json" \
  -d '{
    "authorizerAppId": "wx1234567890abcdef",
    "code": "081XYZ...",
    "phoneCode": "abc123..."
  }'
```

**预期响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "...",
    "expiresIn": 7200,
    "userId": 123456,
    "phoneNumber": "13812345678"
  }
}
```

## 验收标准

✅ **编译通过**：`AuthorizationInfo` 类已补齐，`QueryAuthResult` 可正常编译

✅ **不再抛出 UnsupportedOperationException**：`refreshAuthorizerToken` 已完整实现

✅ **小程序登录可用**：
- code → code2Session 拿到 openid/session_key
- phoneCode → getPhoneNumberByCode 拿到手机号
- 后续用户创建/发 token 逻辑无需修改

✅ **日志完整**：关键操作都有日志，敏感信息已脱敏

✅ **异常处理完善**：所有微信接口错误都会打印 errcode/errmsg

## 注意事项

1. **Token 刷新策略**：
   - `component_access_token`：有效期 2 小时，提前 2 分钟刷新
   - `authorizer_access_token`：有效期 2 小时，提前 2 分钟刷新
   - 刷新时可能返回新的 `refresh_token`，必须覆盖旧的

2. **第三方平台模式特殊性**：
   - 小程序登录必须使用 `component_access_token`，不能使用普通的 `access_token`
   - 获取手机号必须使用授权方的 `authorizer_access_token`
   - 所有接口都需要携带 `component_appid`

3. **错误处理**：
   - 所有微信接口错误都会转换为 `IllegalStateException`
   - 错误信息包含 errcode 和 errmsg，方便排查问题
   - 常见错误码：
     - `40001`：access_token 无效或过期
     - `40013`：appid 无效
     - `61023`：code 已使用或过期
     - `47001`：POST 数据格式错误

4. **安全性**：
   - 所有日志中的敏感信息（appId、token、openId、手机号）都已脱敏
   - appId：只显示前 6 位和后 4 位
   - token/code：只显示前 8 位
   - openId：只显示前 8 位
   - 手机号：只显示前 3 位和后 4 位

## 后续优化建议

1. **缓存优化**：可以考虑在内存中缓存 token（配合 DB 持久化），减少 DB 查询
2. **并发控制**：token 刷新时可以加分布式锁，避免并发刷新
3. **监控告警**：token 刷新失败时应该触发告警
4. **重试机制**：微信接口调用失败时可以考虑重试（注意幂等性）

## 相关文档

- [微信开放平台文档](https://developers.weixin.qq.com/doc/oplatform/openApi/OpenApiDoc/authorization-management/getAuthorizerInfo.html)
- [微信小程序登录文档](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html)
- [微信小程序获取手机号文档](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-info/phone-number/getPhoneNumber.html)
- [WxJava 开源项目](https://github.com/Wechat-Group/WxJava)

