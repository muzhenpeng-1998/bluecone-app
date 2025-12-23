# app-wechat 模块

## 概述

`app-wechat` 模块是 BlueCone 应用的微信集成模块，负责处理所有与微信相关的功能，包括：

- **微信开放平台（第三方平台）**：管理第三方平台授权、获取授权小程序信息
- **微信小程序登录**：处理小程序登录、手机号获取等功能

## 模块结构

```
app-wechat/
├── src/main/java/com/bluecone/app/wechat/
│   ├── config/                          # 配置类
│   │   ├── WeChatClientConfiguration.java    # 客户端配置（Stub vs WxJava）
│   │   └── WxJavaOpenConfiguration.java      # WxJava 开放平台配置
│   ├── openplatform/                    # 开放平台相关
│   │   └── WxJavaOpenPlatformClient.java     # 开放平台客户端实现
│   └── miniapp/                         # 小程序相关
│       ├── WxJavaMiniAppClient.java          # 小程序客户端实现
│       └── WxMaServiceFactory.java           # 小程序服务工厂
└── src/test/java/com/bluecone/app/wechat/
    ├── WechatIntegrationTest.java       # 集成测试
    └── WechatTestConfiguration.java     # 测试配置
```

## 主要功能

### 1. 微信开放平台（第三方平台）

#### 1.1 接口实现

- `WxJavaOpenPlatformClient`：基于 WxJava SDK 的开放平台客户端实现
  - `getComponentAccessToken()`：获取第三方平台 access_token
  - `createPreAuthCode()`：创建预授权码
  - `queryAuth()`：使用授权码查询授权信息
  - `getAuthorizerInfo()`：获取授权方（小程序）基本信息

#### 1.2 配置管理

- `WxJavaOpenConfiguration`：WxJava 开放平台配置类
  - 初始化 `WxOpenService`
  - 配置 component_verify_ticket 提供器
  - 支持 Redis 存储配置（生产环境推荐）

### 2. 微信小程序登录

#### 2.1 接口实现

- `WxJavaMiniAppClient`：基于 WxJava SDK 的小程序客户端实现
  - `code2Session()`：使用 code 换取 openId、unionId、sessionKey
  - `decryptPhoneNumber()`：解密手机号（支持 phoneCode 和 encryptedData/iv 两种方式）

#### 2.2 服务管理

- `WxMaServiceFactory`：小程序服务工厂
  - 为每个授权的小程序创建独立的 `WxMaService` 实例
  - 使用 `ConcurrentHashMap` 缓存实例，提高性能
  - 从数据库查询授权信息并动态创建服务

### 3. 客户端配置

- `WeChatClientConfiguration`：客户端配置类
  - 根据 `wechat.open-platform.enabled` 配置决定使用哪种实现
  - `enabled=true`：使用 WxJava 实现（生产环境）
  - `enabled=false`：使用 Stub 实现（开发/测试环境）

## 依赖关系

### 内部依赖

- `app-core`：核心领域模型和异常定义
- `app-infra`：基础设施层，包括数据库访问、微信接口定义

### 外部依赖

- `weixin-java-open`：WxJava 微信开放平台 SDK（版本 4.7.9.B）
- `weixin-java-miniapp`：WxJava 微信小程序 SDK（版本 4.7.9.B）
- `spring-boot-starter`：Spring Boot 核心依赖
- `spring-boot-starter-web`：Spring Web 依赖

## 配置说明

### 开发环境配置

```yaml
wechat:
  open-platform:
    enabled: false  # 使用 Stub 实现，不需要真实配置
```

### 生产环境配置

```yaml
wechat:
  open-platform:
    enabled: true
    component-app-id: ${WECHAT_COMPONENT_APP_ID}
    component-app-secret: ${WECHAT_COMPONENT_APP_SECRET}
    component-token: ${WECHAT_COMPONENT_TOKEN}
    component-aes-key: ${WECHAT_COMPONENT_AES_KEY}
```

**注意：** 生产环境的敏感配置必须通过环境变量注入，不要硬编码在配置文件中。

## 使用示例

### 1. 注入客户端

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final WeChatOpenPlatformClient openPlatformClient;
    private final WeChatMiniAppClient miniAppClient;
    
    // 使用客户端...
}
```

### 2. 获取预授权码

```java
PreAuthCodeResult result = openPlatformClient.createPreAuthCode(componentAccessToken);
if (result.isSuccess()) {
    String preAuthCode = result.getPreAuthCode();
    // 拼接授权页 URL
}
```

### 3. 小程序登录

```java
WeChatCode2SessionResult sessionResult = miniAppClient.code2Session(appId, code);
String openId = sessionResult.getOpenId();
String unionId = sessionResult.getUnionId();
```

### 4. 获取手机号

```java
// 方式一：使用 phoneCode（推荐）
WeChatPhoneNumberResult phoneResult = miniAppClient.decryptPhoneNumber(
    appId, phoneCode, null, null);

// 方式二：使用 encryptedData/iv（兼容旧版本）
WeChatPhoneNumberResult phoneResult = miniAppClient.decryptPhoneNumber(
    appId, sessionKey, encryptedData, iv);
```

## 测试

### 运行测试

```bash
mvn test -pl app-wechat
```

### 集成测试

- `WechatIntegrationTest`：验证模块可以正常加载和初始化

## 注意事项

### 1. 安全注意事项

- **敏感配置保护**：`component-app-secret`、`component-token`、`component-aes-key` 必须通过环境变量注入
- **Token 安全**：不要将 token 暴露给客户端
- **签名校验**：所有微信回调接口必须进行签名校验

### 2. 性能优化

- **服务缓存**：`WxMaServiceFactory` 使用 `ConcurrentHashMap` 缓存 `WxMaService` 实例
- **Token 缓存**：WxJava 内部会缓存 access_token，避免频繁请求微信接口

### 3. 错误处理

- 所有微信接口调用都会捕获 `WxErrorException` 并转换为业务异常
- 详细的错误日志可以帮助排查问题

### 4. 开发调试

- 开发环境建议使用 Stub 实现，方便本地调试
- 测试环境需要配置真实的微信开放平台凭证
- 可以使用内网穿透工具（如 ngrok）将本地服务暴露到公网

## 相关文档

- [微信集成指南](../docs/wechat-integration-guide.md)
- [微信开放平台官方文档](https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/2.0/getting_started/how_to_read.html)
- [WxJava 官方文档](https://github.com/Wechat-Group/WxJava)

## 维护者

BlueCone Architecture Team

## 版本历史

- **v1.0.0** (2025-12-22)
  - 初始版本
  - 实现微信开放平台基本功能
  - 实现微信小程序登录功能
  - 支持手机号获取（phoneCode 和 encryptedData/iv 两种方式）

