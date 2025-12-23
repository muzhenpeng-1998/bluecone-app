# WeChat Open Platform Configuration Guide

## Overview

This guide explains how to configure the WeChat Open Platform integration in the bluecone-app.

## Configuration Parameters

### Enable/Disable WeChat Integration

The WeChat integration can be toggled between real WxJava implementation and Stub implementation:

```yaml
wechat:
  open-platform:
    enabled: true  # Set to false to use Stub implementation
```

- **`enabled=true`**: Uses real WxJava SDK to call WeChat APIs
- **`enabled=false`** (or not set): Uses Stub implementation for development/testing

### Component Parameters

When `enabled=true`, you must provide the following component parameters:

```yaml
wechat:
  open-platform:
    enabled: true
    component-app-id: your_component_app_id
    component-app-secret: your_component_app_secret
    component-token: your_component_token
    component-aes-key: your_component_aes_key
```

#### Parameter Descriptions

| Parameter | Description | Required |
|-----------|-------------|----------|
| `component-app-id` | Third-party platform AppID | Yes |
| `component-app-secret` | Third-party platform AppSecret | Yes |
| `component-token` | Message verification Token | Yes |
| `component-aes-key` | Message encryption/decryption Key | Yes |

## Configuration Examples

### Development Environment (application-local.yml)

```yaml
wechat:
  open-platform:
    enabled: false  # Use Stub for local development
```

### Production Environment (application-prod.yml)

```yaml
wechat:
  open-platform:
    enabled: true
    component-app-id: ${WECHAT_COMPONENT_APP_ID}
    component-app-secret: ${WECHAT_COMPONENT_APP_SECRET}
    component-token: ${WECHAT_COMPONENT_TOKEN}
    component-aes-key: ${WECHAT_COMPONENT_AES_KEY}
```

**Note**: Use environment variables or secure configuration management for sensitive credentials in production.

## How It Works

### Bean Injection

The system uses conditional bean injection based on the `enabled` flag:

1. **When `enabled=true`**:
   - `WxOpenService` bean is created
   - `WxJavaWeChatOpenPlatformClient` is injected
   - `WxJavaWeChatMiniAppClient` is injected

2. **When `enabled=false`**:
   - `WeChatOpenPlatformClientStub` is injected
   - `WeChatMiniAppClientStub` is injected

### WxOpenService Initialization

When enabled, the `WxOpenService` is initialized with:

1. Component credentials from configuration
2. Latest `component_verify_ticket` from database (if available)
3. In-memory configuration storage

## Obtaining Component Credentials

To obtain WeChat Open Platform component credentials:

1. Register as a third-party platform at [WeChat Open Platform](https://open.weixin.qq.com/)
2. Create a third-party platform application
3. Obtain the following from the platform console:
   - Component AppID
   - Component AppSecret
   - Message verification Token
   - Message encryption/decryption Key

## Troubleshooting

### Issue: Application fails to start with "component_verify_ticket missing"

**Cause**: The `component_verify_ticket` hasn't been received from WeChat yet.

**Solution**:
1. Ensure your callback URL is correctly configured in WeChat Open Platform
2. Wait for WeChat to push the `component_verify_ticket` (usually every 10 minutes)
3. Check the `wechat_component_credential` table for the ticket

### Issue: "WxJava methods not implemented" errors

**Cause**: Some WxJava API methods need to be implemented based on the actual WxJava 4.7.0 API.

**Solution**:
1. Check the TODO comments in the code
2. Refer to [WxJava documentation](https://github.com/Wechat-Group/WxJava)
3. Implement the methods according to the actual API signatures

## Security Considerations

1. **Never commit credentials**: Always use environment variables or secure vaults
2. **Rotate secrets regularly**: Update component secrets periodically
3. **Monitor access logs**: Track API calls and authorization events
4. **Use HTTPS**: Ensure all WeChat callbacks use HTTPS

## Related Files

- Configuration: `app-wechat/src/main/java/com/bluecone/app/wechat/config/WeChatClientConfiguration.java`
- Properties: `app-wechat/src/main/java/com/bluecone/app/wechat/config/WeChatOpenPlatformProperties.java`
- Open Platform Client: `app-wechat/src/main/java/com/bluecone/app/wechat/openplatform/WxJavaWeChatOpenPlatformClient.java`
- Mini App Client: `app-wechat/src/main/java/com/bluecone/app/wechat/miniapp/WxJavaWeChatMiniAppClient.java`

## References

- [WeChat Open Platform Documentation](https://developers.weixin.qq.com/doc/oplatform/en/Third-party_Platforms/2.0/api/Before_Develop/Technical_Plan.html)
- [WxJava GitHub Repository](https://github.com/Wechat-Group/WxJava)
- [WxJava Open Platform Documentation](https://github.com/Wechat-Group/WxJava/wiki/MP_OAuth2%E7%BD%91%E9%A1%B5%E6%8E%88%E6%9D%83)

