# 微信支付 V3 服务商模式测试指南

## 前置准备

### 1. 配置检查

```bash
# 检查配置文件是否正确
grep -A 10 "bluecone.wechat.pay" application.yml

# 或检查环境变量
env | grep BLUECONE_WECHAT_PAY
```

必须配置的项：
- ✅ `enabled=true`
- ✅ `sp-app-id`（服务商 appid）
- ✅ `sp-mch-id`（服务商商户号）
- ✅ `api-v3-key`（32位密钥）
- ✅ `cert-serial-no`（证书序列号）
- ✅ `private-key-path` 或 `private-key-string`
- ✅ `default-notify-url`（回调地址）

### 2. 数据库检查

```sql
-- 检查支付渠道配置
SELECT 
    tenant_id,
    store_id,
    channel_type,
    enabled,
    JSON_EXTRACT(wechat_secrets, '$.subMchId') AS sub_mch_id,
    notify_url
FROM bc_payment_channel_config
WHERE channel_type = 'WECHAT_JSAPI';

-- 检查租户授权小程序
SELECT 
    tenant_id,
    authorizer_app_id,
    authorization_status
FROM bc_wechat_authorized_app
WHERE authorization_status = 'AUTHORIZED';
```

必须满足的条件：
- ✅ 有 WECHAT_JSAPI 配置且 `enabled=1`
- ✅ `sub_mch_id` 已填写
- ✅ 租户有 AUTHORIZED 状态的小程序

### 3. 应用启动检查

```bash
# 查看启动日志，确认 WxPayService 已初始化
tail -f logs/application.log | grep WxJavaWeChatPayConfiguration
```

期望日志：
```
[WxJavaWeChatPayConfiguration] 初始化微信支付服务商模式，spMchId=1234567890
[WxJavaWeChatPayConfiguration] 从文件加载商户私钥，path=/path/to/apiclient_key.pem
[WxJavaWeChatPayConfiguration] 微信支付服务商模式初始化完成
```

如果看到以下日志，说明使用了 stub（本地开发模式）：
```
[StubWeChatPaymentGateway] 使用占位实现，不调用真实微信接口
```

## 测试步骤

### 步骤 1：创建支付单

#### 1.1 API 请求示例

```bash
curl -X POST http://localhost:8080/api/payment/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "tenantId": 1,
    "storeId": 1,
    "userId": 123,
    "amount": 0.01,
    "currency": "CNY",
    "description": "测试订单",
    "channel": "WECHAT",
    "method": "WECHAT_JSAPI",
    "payerOpenId": "oABC123456789",
    "attach": "{\"orderId\":\"ORDER_123\"}"
  }'
```

#### 1.2 期望响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentId": 456,
    "outTradeNo": "PAY_456",
    "status": "PENDING",
    "jsapiPayInfo": {
      "appId": "wx5678901234abcdef",
      "timeStamp": "1234567890",
      "nonceStr": "abc123xyz",
      "packageValue": "prepay_id=wx123456789",
      "signType": "RSA",
      "paySign": "..."
    }
  }
}
```

#### 1.3 关键日志

```
[WxJavaWeChatPaymentGateway] 开始服务商 JSAPI 预下单，tenantId=1, paymentOrderId=456, outTradeNo=PAY_456
[WxJavaWeChatPaymentGateway] 子商户信息：subMchId=1234***7890, subAppId=wx5678***cdef
[WxJavaWeChatPaymentGateway] 服务商下单成功，outTradeNo=PAY_456, prepayId=prepay_id=wx123456789
```

#### 1.4 常见错误

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| 子商户号配置缺失 | `sub_mch_id` 为空 | 检查 `bc_payment_channel_config.wechat_secrets` |
| 该租户未授权小程序 | 租户没有 AUTHORIZED 小程序 | 检查 `bc_wechat_authorized_app` |
| 回调地址配置缺失 | `notify_url` 和 `defaultNotifyUrl` 都为空 | 配置回调地址 |
| PARAM_ERROR | 参数错误 | 检查金额、openid、商户号等参数 |
| APPID_MCHID_NOT_MATCH | appid 与商户号不匹配 | 检查服务商配置 |

### 步骤 2：小程序端唤起支付

#### 2.1 小程序代码示例

```javascript
// 从后端获取 jsapiPayInfo
const payInfo = response.data.jsapiPayInfo;

// 唤起微信支付
wx.requestPayment({
  timeStamp: payInfo.timeStamp,
  nonceStr: payInfo.nonceStr,
  package: payInfo.packageValue,
  signType: payInfo.signType,
  paySign: payInfo.paySign,
  success: function(res) {
    console.log('支付成功', res);
    // 跳转到支付成功页面
  },
  fail: function(err) {
    console.error('支付失败', err);
    // 处理支付失败
  }
});
```

#### 2.2 常见错误

| 错误码 | 错误信息 | 原因 | 解决方案 |
|-------|---------|------|---------|
| -1 | 系统错误 | 签名错误或参数错误 | 检查 paySign 是否正确 |
| -2 | 用户取消支付 | 用户点击取消 | 正常流程，无需处理 |
| -3 | 支付失败 | 余额不足或其他原因 | 提示用户重试 |
| -4 | 网络错误 | 网络连接失败 | 提示用户检查网络 |

### 步骤 3：支付成功（真实环境）

在真实环境中，用户完成支付后，微信会异步推送回调到 `notify_url`。

#### 3.1 回调请求示例

微信会发送 POST 请求到：
```
https://your-domain.com/open-api/wechat/pay/notify
```

HTTP 头：
```
Wechatpay-Timestamp: 1234567890
Wechatpay-Nonce: abc123xyz
Wechatpay-Signature: ...
Wechatpay-Serial: 1234567890ABCDEF...
```

请求体（加密）：
```json
{
  "id": "...",
  "create_time": "2025-12-25T10:00:00+08:00",
  "resource_type": "encrypt-resource",
  "event_type": "TRANSACTION.SUCCESS",
  "resource": {
    "algorithm": "AEAD_AES_256_GCM",
    "ciphertext": "...",
    "nonce": "...",
    "associated_data": "..."
  }
}
```

#### 3.2 期望日志

```
[WechatPayCallback] 收到微信支付回调，timestamp=1234567890, nonce=abc123, serial=1234567890ABCDEF
[WechatPayCallback] 验签解密成功，outTradeNo=PAY_456, transactionId=4200001234567890, tradeState=SUCCESS
[WechatPayCallback] 回调解析完成，spMchid=1234***7890, subMchid=9876***4321, subAppid=wx5678***cdef, outTradeNo=PAY_456, transactionId=4200001234567890
[wechat-callback] traceId=abc-123, outTradeNo=PAY_456, transactionId=4200001234567890, tradeState=SUCCESS
[wechat-callback] 支付单状态更新成功，paymentId=456, status=SUCCESS
```

#### 3.3 期望响应

```json
{
  "code": "SUCCESS",
  "message": "成功"
}
```

#### 3.4 常见错误

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| 微信支付服务未启用 | `enabled=false` | 设置 `enabled=true` |
| 微信支付回调验签失败 | 密钥或证书错误 | 检查 `apiV3Key`、`certSerialNo`、`privateKey` |
| 支付单不存在 | `outTradeNo` 错误 | 检查订单号是否正确 |
| 金额不一致 | 回调金额与订单金额不匹配 | 检查订单金额 |

### 步骤 4：查询支付结果

#### 4.1 查询支付单状态

```bash
curl -X GET http://localhost:8080/api/payment/456 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 4.2 期望响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentId": 456,
    "outTradeNo": "PAY_456",
    "transactionId": "4200001234567890",
    "status": "SUCCESS",
    "amount": 0.01,
    "paidAt": "2025-12-25T10:00:00Z"
  }
}
```

#### 4.3 数据库查询

```sql
SELECT 
    id,
    out_trade_no,
    transaction_id,
    status,
    payable_amount,
    paid_at
FROM bc_payment_order
WHERE id = 456;
```

期望结果：
```
id  | out_trade_no | transaction_id      | status  | payable_amount | paid_at
456 | PAY_456      | 4200001234567890    | SUCCESS | 0.01           | 2025-12-25 10:00:00
```

## 本地测试（使用 stub）

如果没有真实的微信支付密钥，可以使用 stub 模式进行本地测试：

### 1. 配置

```yaml
bluecone:
  wechat:
    pay:
      enabled: false  # 使用 StubWeChatPaymentGateway
```

### 2. 创建支付单

请求同上，响应会返回占位数据：

```json
{
  "jsapiPayInfo": {
    "appId": "wx...",
    "timeStamp": "1234567890",
    "nonceStr": "stub-nonce",
    "packageValue": "prepay_id=stub",
    "signType": "RSA",
    "paySign": "stub-sign"
  }
}
```

### 3. 模拟回调

由于 stub 不会真正调用微信接口，需要手动触发回调：

```bash
curl -X POST http://localhost:8080/open-api/wechat/pay/notify \
  -H "Content-Type: application/json" \
  -H "Wechatpay-Timestamp: 1234567890" \
  -H "Wechatpay-Nonce: abc123" \
  -H "Wechatpay-Signature: stub-signature" \
  -H "Wechatpay-Serial: stub-serial" \
  -d '{
    "id": "stub-notify-id",
    "create_time": "2025-12-25T10:00:00+08:00",
    "resource_type": "encrypt-resource",
    "event_type": "TRANSACTION.SUCCESS",
    "resource": {
      "ciphertext": "stub-ciphertext"
    }
  }'
```

**注意**：stub 模式下回调会失败（因为无法验签），需要在 `WechatPayCallbackController` 中添加 stub 模式支持。

## 生产环境测试

### 1. 使用微信支付沙箱环境

微信支付提供沙箱环境用于测试，无需真实支付：

- 沙箱商户号：从微信商户平台获取
- 沙箱密钥：从微信商户平台获取
- 沙箱证书：从微信商户平台下载

配置沙箱环境后，可以使用测试金额（如 0.01 元）进行测试。

### 2. 使用 ngrok 暴露本地服务

如果需要在本地接收微信回调，可以使用 ngrok：

```bash
# 安装 ngrok
brew install ngrok

# 启动 ngrok
ngrok http 8080

# 复制 ngrok 提供的公网地址
# 例如：https://abc123.ngrok.io

# 更新回调地址配置
# notify_url: https://abc123.ngrok.io/open-api/wechat/pay/notify
```

### 3. 查看回调日志

```bash
# 实时查看回调日志
tail -f logs/application.log | grep -E "WechatPayCallback|wechat-callback"

# 查看错误日志
tail -f logs/application.log | grep -E "ERROR|WARN" | grep -i wechat
```

## 性能测试

### 1. 并发下单测试

```bash
# 使用 ab（Apache Bench）进行并发测试
ab -n 100 -c 10 -T "application/json" -p payment-request.json \
  http://localhost:8080/api/payment/create
```

### 2. 回调处理性能测试

```bash
# 使用 wrk 进行压力测试
wrk -t 4 -c 100 -d 30s --latency \
  -s callback-script.lua \
  http://localhost:8080/open-api/wechat/pay/notify
```

### 3. 监控指标

- 下单成功率：应 > 99%
- 下单平均耗时：应 < 500ms
- 回调处理成功率：应 > 99.9%
- 回调处理平均耗时：应 < 100ms

## 故障排查

### 1. 下单失败

```bash
# 查看下单日志
tail -f logs/application.log | grep WxJavaWeChatPaymentGateway

# 查看微信接口错误
tail -f logs/application.log | grep "WxPayException"
```

### 2. 回调失败

```bash
# 查看回调日志
tail -f logs/application.log | grep WechatPayCallback

# 查看验签错误
tail -f logs/application.log | grep "验签或解密失败"
```

### 3. 数据库问题

```sql
-- 查看未完成的支付单
SELECT 
    id,
    out_trade_no,
    status,
    created_at,
    TIMESTAMPDIFF(MINUTE, created_at, NOW()) AS minutes_ago
FROM bc_payment_order
WHERE status IN ('PENDING', 'PROCESSING')
  AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY created_at DESC;

-- 查看失败的支付单
SELECT 
    id,
    out_trade_no,
    status,
    error_message,
    created_at
FROM bc_payment_order
WHERE status = 'FAILED'
  AND created_at > DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY created_at DESC;
```

## 验收标准

- ✅ 配置正确，应用启动成功
- ✅ 下单接口返回有效的 JSAPI payInfo
- ✅ 小程序端能唤起支付
- ✅ 支付成功后回调命中 `/open-api/wechat/pay/notify`
- ✅ 回调验签解密成功
- ✅ 支付单状态更新为 SUCCESS
- ✅ 发布 PaymentSucceededEvent
- ✅ 日志完整，敏感信息已脱敏
- ✅ 异常处理完善，错误信息清晰

## 相关文档

- [微信支付 V3 开发文档](https://pay.weixin.qq.com/wiki/doc/apiv3_partner/index.shtml)
- [微信支付沙箱环境](https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=23_1)
- [WxJava 文档](https://github.com/Wechat-Group/WxJava/wiki)
- [ngrok 文档](https://ngrok.com/docs)

