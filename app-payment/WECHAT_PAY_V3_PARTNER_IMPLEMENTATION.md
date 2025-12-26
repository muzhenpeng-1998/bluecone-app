# 微信支付 V3 服务商（Partner）JSAPI 支付实现说明

## 概述

本次实现打通了「微信支付 V3 服务商模式 JSAPI 支付」的完整闭环，包括：

1. ✅ 服务商下单（partner prepay）
2. ✅ 回调验签解密（V3 signature verification）
3. ✅ 业务入账（payment success handling）
4. ✅ 多租户子商户支持（multi-tenant sub-merchant）

## 实现原则

- **服务商密钥不进 DB**：spAppId、spMchId、apiV3Key、证书等通过配置文件/环境变量注入
- **子商户信息进 DB**：subMchId 存储在 `bc_payment_channel_config.sub_mch_id`
- **sub_appid 动态获取**：从 `WechatAuthorizedAppService` 按 tenantId 获取已授权小程序
- **复用 WxJava**：使用 `weixin-java-pay` SDK 处理签名、加密、证书管理，降低代码复杂度
- **V3 验签解密**：回调必须走 V3 头部验签 + 解密，不再假设"已解密"

## 文件变更清单

### 1. app-payment/pom.xml
**变更**：新增 `weixin-java-pay` 依赖

```xml
<dependency>
    <groupId>com.github.binarywang</groupId>
    <artifactId>weixin-java-pay</artifactId>
    <version>${wx-java.version}</version>
</dependency>
```

### 2. app-payment/src/main/java/com/bluecone/app/payment/infrastructure/wechatpay/BlueconeWeChatPayProperties.java
**变更**：新增配置类

**说明**：封装服务商密钥配置，包含：
- `enabled`：是否启用微信支付（默认 false，本地开发可关闭）
- `spAppId`：服务商 appid（sp_appid）
- `spMchId`：服务商商户号（sp_mchid）
- `apiV3Key`：V3 API KEY（32位）
- `certSerialNo`：商户证书序列号
- `privateKeyPath` / `privateKeyString`：商户私钥（二选一）
- `defaultNotifyUrl`：默认回调地址

### 3. app-payment/src/main/java/com/bluecone/app/payment/infrastructure/wechatpay/WxJavaWeChatPayConfiguration.java
**变更**：新增配置类

**说明**：构建 `WxPayService` Bean（服务商单例），配置：
- 仅当 `bluecone.wechat.pay.enabled=true` 时启用
- 从 `BlueconeWeChatPayProperties` 读取配置
- 设置 appId、mchId、apiV3Key、certSerialNo、privateKey
- WxJava 会自动处理平台证书下载和缓存

### 4. app-payment/src/main/java/com/bluecone/app/payment/infrastructure/wechatpay/WxJavaWeChatPaymentGateway.java
**变更**：新增实现类（服务商模式）

**实现方法**：
- `jsapiPrepay()`：服务商 JSAPI 预下单
  1. 校验渠道配置（enabled、channelType=WECHAT_JSAPI）
  2. 从 `config.getWeChatSecrets().getSubMchId()` 获取子商户号
  3. 从 `WechatAuthorizedAppService.getAuthorizerAppIdByTenantId()` 获取 sub_appid
  4. 构造 `WxPayUnifiedOrderV3Request`（服务商模式）：
     - spAppid / spMchid：服务商信息
     - subAppid / subMchid：子商户信息
     - description / outTradeNo / notifyUrl / amount / payer.subOpenid
  5. 调用 `wxPayService.createOrderV3(WxPayConstants.TradeType.JSAPI, request)`
  6. 从结果中获取 `payInfo`（JsapiResult）
  7. 返回 `WeChatJsapiPrepayResponse`（包含 appId/timeStamp/nonceStr/packageValue/signType/paySign）

**异常处理**：
- 子商户号为空：抛 `BusinessException("子商户号配置缺失")`
- 租户未授权小程序：抛 `BusinessException("该租户未授权小程序")`
- 回调地址为空：抛 `BusinessException("回调地址配置缺失")`
- 微信下单失败：抛 `BusinessException("微信支付下单失败: " + errCodeDes)`

**日志输出**：
- 关键字段：tenantId、paymentOrderId、outTradeNo、subMchId、subAppId、prepayId
- 敏感信息脱敏：mchId 显示前4位+后4位，appId 显示前6位+后4位

### 5. app-payment/src/main/java/com/bluecone/app/payment/infrastructure/gateway/StubWeChatPaymentGateway.java
**变更**：添加条件注解

**说明**：
- 添加 `@ConditionalOnProperty(prefix = "bluecone.wechat.pay", name = "enabled", havingValue = "false", matchIfMissing = true)`
- 当 `enabled=false` 或未配置时使用 stub，否则使用 `WxJavaWeChatPaymentGateway`

### 6. app-payment/src/main/java/com/bluecone/app/payment/api/WechatPayCallbackCommand.java
**变更**：新增服务商模式字段

**新增字段**：
- `subAppId`：子商户应用 ID（服务商模式下的 sub_appid）
- `subMchId`：子商户号（服务商模式下的 sub_mchid）
- `payerOpenId`：支付者 openid（服务商模式下为 sub_openid）

### 7. app-application/src/main/java/com/bluecone/app/api/integration/payment/WechatPayCallbackController.java
**变更**：完全重写为 V3 验签解密模式

**实现方法**：
- `payNotify()`：接收微信 V3 回调
  1. 从 HTTP 头读取签名信息：
     - `Wechatpay-Timestamp`
     - `Wechatpay-Nonce`
     - `Wechatpay-Signature`
     - `Wechatpay-Serial`
  2. 构造 `SignatureHeader`
  3. 调用 `wxPayService.parseOrderNotifyV3Result(body, signatureHeader)`
  4. 验签成功后，从 `WxPayOrderNotifyV3Result` 解析字段：
     - sp_appid / sp_mchid（服务商信息）
     - sub_appid / sub_mchid（子商户信息）
     - out_trade_no / transaction_id / trade_state / bank_type / attach
     - amount.total / payer.sub_openid / success_time
  5. 转换为 `WechatPayCallbackCommand`
  6. 调用 `WechatPayCallbackApplicationService.handleWechatPayCallback(command)`
  7. 返回 `{"code":"SUCCESS","message":"成功"}`

**异常处理**：
- `WxPayService` 未启用：抛 `BusinessException("微信支付服务未启用")`
- 验签失败：抛 `BusinessException("微信支付回调验签失败: " + errCodeDes)`
- 业务处理失败：返回 `{"code":"FAIL","message":"业务处理失败"}`

**日志输出**：
- 关键字段：timestamp、nonce、serial、outTradeNo、transactionId、tradeState、spMchid、subMchid、subAppid
- 敏感信息脱敏

### 8. app-payment/src/main/java/com/bluecone/app/payment/domain/channel/WeChatChannelSecrets.java
**变更**：新增 `channelMode` 字段（可选）

**说明**：
- `channelMode`：渠道模式，默认 `"SERVICE_PROVIDER"`（服务商）
- 未来如需同时支持 DIRECT（直连），可根据此字段路由到不同的下单逻辑

### 9. app-infra/src/main/java/com/bluecone/app/infra/wechat/dataobject/WechatAuthorizedAppDO.java
**变更**：修正字段名以匹配数据库 schema

**说明**：
- `authorizerAppid` → `authorizerAppId`
- 新增 `componentAppId`、`authorizerAccessToken`、`authorizerAccessTokenExpireAt`
- `authStatus` → `authorizationStatus`（VARCHAR）
- `firstAuthTime` → `authorizedAt`
- `canceledAt` → `unauthorizedAt`
- 其他字段也调整为与数据库 schema 一致

### 10. app-infra/src/main/java/com/bluecone/app/infra/wechat/openplatform/WechatAuthorizedAppService.java
**变更**：使用正确的字段名查询

**说明**：
- 查询条件：`authorization_status = 'AUTHORIZED'`
- 返回 `authorizerAppId`

## 配置要求

确保 `application.yml` 中配置了以下项：

```yaml
bluecone:
  wechat:
    pay:
      enabled: true  # 启用微信支付（本地开发可设为 false）
      sp-app-id: wx1234567890abcdef  # 服务商 appid
      sp-mch-id: 1234567890  # 服务商商户号
      api-v3-key: your_32_char_api_v3_key_here  # V3 API KEY（32位）
      cert-serial-no: 1234567890ABCDEF  # 商户证书序列号
      private-key-path: /path/to/apiclient_key.pem  # 商户私钥文件路径（推荐）
      # private-key-string: "-----BEGIN PRIVATE KEY-----\n..."  # 或直接配置私钥内容
      default-notify-url: https://your-domain.com/open-api/wechat/pay/notify  # 默认回调地址
```

## 数据库配置要求

### 1. bc_payment_channel_config
确保至少有一条记录：
- `tenant_id`：租户 ID
- `store_id`：门店 ID
- `channel_type`：`WECHAT_JSAPI`
- `status`：`1`（启用）
- `sub_mch_id`：子商户号（存储在 wechat_secrets JSON 中）
- `notify_url`：回调地址（可选，为空则用 defaultNotifyUrl）

示例（假设 wechat_secrets 存储为 JSON）：
```json
{
  "subMchId": "1234567890",
  "channelMode": "SERVICE_PROVIDER"
}
```

### 2. bc_wechat_authorized_app
确保租户已授权小程序：
- `tenant_id`：租户 ID
- `authorizer_app_id`：小程序 appid（sub_appid）
- `authorization_status`：`AUTHORIZED`

## 关键日志示例

### 1. 服务商下单
```
[WxJavaWeChatPaymentGateway] 开始服务商 JSAPI 预下单，tenantId=123, paymentOrderId=456, outTradeNo=PAY_789
[WxJavaWeChatPaymentGateway] 子商户信息：subMchId=1234***7890, subAppId=wx1234***cdef
[WxJavaWeChatPaymentGateway] 服务商下单成功，outTradeNo=PAY_789, prepayId=prepay_id=wx123456789
```

### 2. 回调验签解密
```
[WechatPayCallback] 收到微信支付回调，timestamp=1234567890, nonce=abc123, serial=1234567890ABCDEF
[WechatPayCallback] 验签解密成功，outTradeNo=PAY_789, transactionId=4200001234567890, tradeState=SUCCESS
[WechatPayCallback] 回调解析完成，spMchid=1234***7890, subMchid=9876***4321, subAppid=wx5678***9abc, outTradeNo=PAY_789, transactionId=4200001234567890
```

### 3. 业务入账
```
[wechat-callback] traceId=abc-123, outTradeNo=PAY_789, transactionId=4200001234567890, tradeState=SUCCESS
[wechat-callback] 支付单状态更新成功，paymentId=456, status=SUCCESS
```

## 最小联调 checklist

### 1. 配置检查
- [ ] `bluecone.wechat.pay.enabled=true`
- [ ] 服务商密钥配置正确（spAppId、spMchId、apiV3Key、certSerialNo、privateKeyPath）
- [ ] 默认回调地址配置正确（或在 PaymentChannelConfig 中配置）

### 2. 数据库检查
- [ ] `bc_payment_channel_config` 有 WECHAT_JSAPI 配置，且 sub_mch_id 已填写
- [ ] `bc_wechat_authorized_app` 有租户授权记录，且 authorization_status=AUTHORIZED

### 3. 下单测试
- [ ] 调用创建支付单接口，返回 JSAPI payInfo（appId/timeStamp/nonceStr/packageValue/signType/paySign）
- [ ] 小程序端 `wx.requestPayment(payInfo)` 能唤起支付

### 4. 回调测试
- [ ] 支付成功后，微信回调命中 `/open-api/wechat/pay/notify`
- [ ] 回调验签解密成功，日志输出 outTradeNo、transactionId、tradeState
- [ ] `WechatPayCallbackApplicationService` 将 PaymentOrder 置为 SUCCESS
- [ ] 发布 `PaymentSucceededEvent`

## 异常排查

### 1. 下单失败
- **子商户号为空**：检查 `bc_payment_channel_config.sub_mch_id` 是否填写
- **租户未授权小程序**：检查 `bc_wechat_authorized_app` 是否有 AUTHORIZED 记录
- **回调地址为空**：检查 `defaultNotifyUrl` 或 `PaymentChannelConfig.notifyUrl` 是否配置
- **微信返回错误**：查看日志中的 errCode 和 errCodeDes，常见错误码：
  - `PARAM_ERROR`：参数错误（检查金额、openid、商户号等）
  - `APPID_MCHID_NOT_MATCH`：appid 与商户号不匹配
  - `INVALID_REQUEST`：请求格式错误

### 2. 回调验签失败
- **WxPayService 未启用**：检查 `bluecone.wechat.pay.enabled=true`
- **签名验证失败**：检查 apiV3Key、certSerialNo、privateKey 是否正确
- **平台证书未下载**：WxJava 会自动下载，首次可能需要几秒钟
- **时间戳过期**：检查服务器时间是否正确

### 3. 业务入账失败
- **支付单不存在**：检查 outTradeNo 是否正确
- **金额不一致**：检查回调金额与订单金额是否匹配
- **重复通知**：微信会重复推送，业务层需幂等处理

## 后续优化建议

1. **证书自动更新**：WxJava 默认支持，无需手动处理
2. **分布式锁**：回调处理时加分布式锁，避免并发问题
3. **监控告警**：下单失败、回调验签失败时触发告警
4. **重试机制**：微信接口调用失败时可重试（注意幂等性）
5. **直连模式支持**：未来如需支持直连模式，可根据 `channelMode` 路由到不同实现

## 相关文档

- [微信支付 V3 服务商文档](https://pay.weixin.qq.com/wiki/doc/apiv3_partner/index.shtml)
- [微信支付 V3 JSAPI 下单](https://pay.weixin.qq.com/wiki/doc/apiv3_partner/apis/chapter4_1_1.shtml)
- [微信支付 V3 回调通知](https://pay.weixin.qq.com/wiki/doc/apiv3_partner/apis/chapter4_1_5.shtml)
- [WxJava 开源项目](https://github.com/Wechat-Group/WxJava)

## 验收标准

✅ **编译通过**：所有新增类可正常编译，无错误

✅ **条件装配正确**：
- `enabled=true` 时使用 `WxJavaWeChatPaymentGateway`
- `enabled=false` 时使用 `StubWeChatPaymentGateway`

✅ **服务商下单可用**：
- 能正确获取 sub_appid（从 WechatAuthorizedAppService）
- 能正确构造 partner 下单请求
- 返回有效的 JSAPI payInfo

✅ **回调验签解密可用**：
- 能正确读取 V3 签名头
- 能正确验签和解密
- 能正确解析 partner 回调结构

✅ **业务入账可用**：
- 能正确更新 PaymentOrder 状态
- 能正确发布 PaymentSucceededEvent

✅ **日志完整**：关键操作都有日志，敏感信息已脱敏

✅ **异常处理完善**：所有微信接口错误都会打印 errcode/errmsg

## 注意事项

1. **服务商模式特殊性**：
   - 下单必须走 `/v3/pay/partner/transactions/jsapi`
   - 回调必须走 partner 结构（sp_appid/sp_mchid/sub_appid/sub_mchid）
   - payer.openid 必须是子商户小程序的 openid（sub_openid）

2. **安全性**：
   - 服务商密钥不进 DB，通过配置文件/环境变量注入
   - 回调必须验签，不能假设"已解密"
   - 日志中的敏感信息（mchId、appId）已脱敏

3. **幂等性**：
   - 微信会重复推送回调，业务层需幂等处理
   - `WechatPayCallbackApplicationService` 已实现幂等逻辑

4. **证书管理**：
   - WxJava 会自动下载和缓存平台证书
   - 商户私钥需手动配置（privateKeyPath 或 privateKeyString）

5. **本地开发**：
   - 可设置 `enabled=false` 使用 stub，无需真实密钥
   - 回调测试需公网地址，可使用 ngrok 等工具

