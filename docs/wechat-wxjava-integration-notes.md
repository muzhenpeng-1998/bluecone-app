# WxJava SDK 集成注意事项

## 版本兼容性问题

在集成 WxJava SDK 时遇到了以下问题：

### 问题 1：Maven 仓库版本不可用

阿里云 Maven 仓库中不包含以下版本：
- `4.7.9.B` - 不存在
- `4.7.0` - 不存在  
- `4.6.0` - BOM 不存在，但单独的包可能存在

### 问题 2：API 兼容性

不同版本的 WxJava SDK API 有较大差异，需要根据实际使用的版本调整代码。

## 推荐方案

### 方案 1：使用稳定的老版本（推荐用于快速上线）

使用 Maven 中央仓库可用的稳定版本，如 `4.5.0` 或更早版本：

```xml
<properties>
    <wx-java.version>4.5.0</wx-java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.github.binarywang</groupId>
        <artifactId>weixin-java-open</artifactId>
        <version>${wx-java.version}</version>
    </dependency>
    <dependency>
        <groupId>com.github.binarywang</groupId>
        <artifactId>weixin-java-miniapp</artifactId>
        <version>${wx-java.version}</version>
    </dependency>
</dependencies>
```

### 方案 2：使用 HTTP 客户端直接调用微信 API（推荐用于生产环境）

不依赖 WxJava SDK，直接使用 Spring RestTemplate 或 OkHttp 调用微信 API：

**优点：**
- 完全可控，不受第三方 SDK 版本限制
- 更轻量，只引入必要的依赖
- 更容易调试和排查问题

**缺点：**
- 需要自己实现签名、加密解密等逻辑
- 需要自己处理 token 刷新等

**实现示例：**

```java
@Service
public class WeChatOpenPlatformHttpClient implements WeChatOpenPlatformClient {
    
    private final RestTemplate restTemplate;
    
    @Override
    public ComponentAccessTokenResult getComponentAccessToken(
            String componentAppId,
            String componentAppSecret,
            String componentVerifyTicket) {
        
        String url = "https://api.weixin.qq.com/cgi-bin/component/api_component_token";
        
        Map<String, String> requestBody = Map.of(
            "component_appid", componentAppId,
            "component_appsecret", componentAppSecret,
            "component_verify_ticket", componentVerifyTicket
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, requestBody, Map.class);
        
        // 解析响应...
    }
}
```

### 方案 3：配置 Maven 使用中央仓库

如果必须使用 WxJava SDK，可以配置 Maven 使用中央仓库而不是阿里云镜像：

在 `~/.m2/settings.xml` 或项目 `pom.xml` 中添加：

```xml
<repositories>
    <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
    </repository>
</repositories>
```

## 当前实现状态

当前代码中：

1. **接口定义完整**：
   - `WeChatOpenPlatformClient` - 开放平台客户端接口
   - `WeChatMiniAppClient` - 小程序客户端接口

2. **Stub 实现可用**：
   - `WeChatOpenPlatformClientStub` - 返回模拟数据
   - `WeChatMiniAppClientStub` - 返回模拟数据

3. **HTTP 实现部分完成**：
   - `WeChatOpenPlatformHttpClient` - 基于 RestTemplate 的实现（需要完善）

4. **WxJava 实现待完善**：
   - `WxJavaOpenPlatformClient` - 因版本兼容性问题暂时无法编译
   - `WxJavaMiniAppClient` - 因版本兼容性问题暂时无法编译

## 建议的实施步骤

### 短期（快速上线）

1. 使用 Stub 实现进行开发和测试
2. 完善 `WeChatOpenPlatformHttpClient` 实现
3. 在测试环境验证 HTTP 客户端实现

### 中期（生产环境）

1. 选择方案 2（HTTP 客户端）或方案 1（老版本 WxJava）
2. 完善签名校验、加密解密等安全功能
3. 添加完整的错误处理和重试机制
4. 添加监控和告警

### 长期（持续优化）

1. 根据微信 API 更新及时调整实现
2. 优化性能（缓存、连接池等）
3. 完善测试覆盖率

## 参考资料

- [WxJava GitHub](https://github.com/Wechat-Group/WxJava)
- [微信开放平台官方文档](https://developers.weixin.qq.com/doc/oplatform/Third-party_Platforms/2.0/api/ThirdParty/token/component_access_token.html)
- [微信小程序登录文档](https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html)

