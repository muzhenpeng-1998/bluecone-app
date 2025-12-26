# WxJava 4.7.0 API 使用说明

## 关于 WxJava 版本

本实现使用 **WxJava 4.7.0** 版本，该版本的 API 与一些在线文档或教程中提到的类名可能有所不同。

## 核心类名映射

### 下单相关

| 文档/教程中的类名 | WxJava 4.7.0 实际类名 | 说明 |
|------------------|---------------------|------|
| `WxPayPartnerUnifiedOrderV3Request` | `WxPayPartnerUnifiedOrderV3Request` | 服务商下单请求（✅ 正确） |
| `WxPayPartnerUnifiedOrderV3Request.Amount` | `WxPayPartnerUnifiedOrderV3Request.Amount` | 金额信息内部类（✅ 正确） |
| `WxPayPartnerUnifiedOrderV3Request.Payer` | `WxPayPartnerUnifiedOrderV3Request.Payer` | 支付者信息内部类（✅ 正确） |
| `TradeTypeEnum.JSAPI` | `TradeTypeEnum.JSAPI` | 交易类型枚举（✅ 正确） |

### 下单方法

| 文档/教程中的方法 | WxJava 4.7.0 实际方法 | 说明 |
|-----------------|---------------------|------|
| `wxPayService.createPartnerOrderV3(TradeTypeEnum.JSAPI, request)` | `wxPayService.createPartnerOrderV3(TradeTypeEnum.JSAPI, request)` | 创建服务商订单（✅ 正确） |

**重要说明**：
- `WxPayPartnerUnifiedOrderV3Request` 专门用于服务商模式
- `WxPayUnifiedOrderV3Request` 用于直连模式
- 两者是不同的类，不能混用

### 回调相关

| 文档/教程中的类名 | WxJava 4.7.0 实际类名 | 说明 |
|------------------|---------------------|------|
| `WxPayPartnerNotifyV3Result` | `WxPayPartnerNotifyV3Result` | 服务商回调通知结果（✅ 正确） |
| `WxPayPartnerNotifyV3Result.DecryptNotifyResult` | `WxPayPartnerNotifyV3Result.DecryptNotifyResult` | 解密后的通知结果（✅ 正确） |

### 回调方法

| 文档/教程中的方法 | WxJava 4.7.0 实际方法 | 说明 |
|-----------------|---------------------|------|
| `wxPayService.parsePartnerOrderNotifyV3Result(body, signatureHeader)` | `wxPayService.parsePartnerOrderNotifyV3Result(body, signatureHeader)` | 解析服务商回调通知（✅ 正确） |

**重要说明**：
- `parsePartnerOrderNotifyV3Result` 专门用于服务商模式回调
- `parseOrderNotifyV3Result` 用于直连模式回调
- 解密后的 `DecryptNotifyResult` 包含 `spAppid`、`spMchid`、`subAppid`、`subMchid` 等字段

## 服务商模式下单示例

```java
// 构造请求
WxPayPartnerUnifiedOrderV3Request request = new WxPayPartnerUnifiedOrderV3Request();

// 服务商信息（必填）
request.setSpAppid("wx1234567890abcdef");  // 服务商 appid
request.setSpMchId("1234567890");          // 服务商商户号（注意：setSpMchId，不是 setSpMchid）

// 子商户信息（必填）
request.setSubAppid("wx9876543210fedcba"); // 子商户小程序 appid
request.setSubMchId("0987654321");         // 子商户号（注意：setSubMchId，不是 setSubMchid）

// 订单信息
request.setDescription("测试订单");
request.setOutTradeNo("ORDER_123456");
request.setNotifyUrl("https://your-domain.com/notify");

// 金额信息
WxPayPartnerUnifiedOrderV3Request.Amount amount = new WxPayPartnerUnifiedOrderV3Request.Amount();
amount.setTotal(1);  // 单位：分
amount.setCurrency("CNY");
request.setAmount(amount);

// 支付者信息（使用子商户小程序的 openid）
WxPayPartnerUnifiedOrderV3Request.Payer payer = new WxPayPartnerUnifiedOrderV3Request.Payer();
payer.setSubOpenid("oABC123456789");  // 子商户小程序的 openid
request.setPayer(payer);

// 调用下单接口
WxPayUnifiedOrderV3Result.JsapiResult payInfo = wxPayService.createPartnerOrderV3(
    TradeTypeEnum.JSAPI, 
    request
);

// payInfo 已经包含了所有支付参数（appId、timeStamp、nonceStr、packageValue、signType、paySign）
```

## 服务商模式回调示例

```java
// 构造签名头
SignatureHeader signatureHeader = new SignatureHeader();
signatureHeader.setTimeStamp(wechatpayTimestamp);
signatureHeader.setNonce(wechatpayNonce);
signatureHeader.setSignature(wechatpaySignature);
signatureHeader.setSerial(wechatpaySerial);

// 解析回调通知
WxPayPartnerNotifyV3Result notifyResult = wxPayService.parsePartnerOrderNotifyV3Result(
    body, 
    signatureHeader
);

// 获取解密后的结果
WxPayPartnerNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();

// 服务商信息
String spAppid = result.getSpAppid();
String spMchid = result.getSpMchid();

// 子商户信息
String subAppid = result.getSubAppid();
String subMchid = result.getSubMchid();

// 订单信息
String outTradeNo = result.getOutTradeNo();
String transactionId = result.getTransactionId();
String tradeState = result.getTradeState();

// 金额信息
Integer total = result.getAmount().getTotal();

// 支付者信息
String subOpenid = result.getPayer().getSubOpenid();
```

## 直连模式与服务商模式的区别

### 直连模式（DIRECT）

```java
// 只设置商户信息
request.setAppid("wx1234567890abcdef");
request.setMchid("1234567890");

// 支付者使用普通 openid
payer.setOpenid("oABC123456789");
```

### 服务商模式（SERVICE_PROVIDER）

```java
// 设置服务商和子商户信息
request.setSpAppid("wx1234567890abcdef");  // 服务商
request.setSpMchid("1234567890");
request.setSubAppid("wx9876543210fedcba"); // 子商户
request.setSubMchid("0987654321");

// 支付者使用子商户 openid
payer.setSubOpenid("oABC123456789");
```

## 常见问题

### Q1: `setSpMchid` 还是 `setSpMchId`？

**A**: WxJava 4.7.0 中使用 `setSpMchId`（注意大小写），不是 `setSpMchid`。同样，`setSubMchId` 也是大写 `I`。

### Q2: 服务商模式和直连模式的类有什么区别？

**A**: 
- 服务商模式：使用 `WxPayPartnerUnifiedOrderV3Request` 和 `createPartnerOrderV3`
- 直连模式：使用 `WxPayUnifiedOrderV3Request` 和 `createOrderV3`
- 两者是完全不同的类和方法，不能混用

### Q3: 如何确认使用的是服务商模式？

**A**: 检查请求中是否设置了以下字段：
- `spAppid` 和 `spMchid`（服务商信息）
- `subAppid` 和 `subMchid`（子商户信息）
- `payer.subOpenid`（子商户小程序的 openid）

如果这些字段都已设置，SDK 会自动使用服务商模式的 API。

### Q4: 回调时如何区分直连模式和服务商模式？

**A**: 解析回调结果后，检查 `result.getSpAppid()` 和 `result.getSubAppid()` 是否为空：
- 如果不为空，说明是服务商模式
- 如果为空，说明是直连模式

## 参考资源

- [WxJava GitHub](https://github.com/Wechat-Group/WxJava)
- [WxJava Wiki](https://github.com/Wechat-Group/WxJava/wiki)
- [微信支付 V3 官方文档](https://pay.weixin.qq.com/wiki/doc/apiv3/index.shtml)
- [微信支付服务商模式文档](https://pay.weixin.qq.com/wiki/doc/apiv3_partner/index.shtml)

## 版本兼容性

| WxJava 版本 | 支持的微信支付 API | 备注 |
|------------|------------------|------|
| 4.7.0 | V3 | 本项目使用版本 |
| 4.6.x | V3 | API 可能略有不同 |
| 4.5.x | V3 | API 可能略有不同 |
| 3.x.x | V2 + V3 | 不推荐使用 |

**建议**：使用 4.6.0 及以上版本，以获得最佳的 V3 API 支持。

---

**最后更新**: 2025-12-25  
**文档版本**: v1.0.0

