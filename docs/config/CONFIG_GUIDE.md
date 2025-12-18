# BlueCone 应用配置指南

本文档说明如何在 BlueCone 应用中配置环境变量，特别是在微信云托管等部署环境中的配置方式。

## 配置架构

BlueCone 应用采用多环境配置架构：

- **`application.yml`**: 通用默认配置，不包含敏感信息
- **`application-local.yml`**: 本地开发配置（不提交到版本库）
- **`application-local.example.yml`**: 本地开发配置示例文件
- **`application-dev.yml`**: 开发环境配置骨架（仅占位符）
- **`application-prod.yml`**: 生产环境配置骨架（仅占位符）

### 配置优先级

Spring Boot 配置优先级（从高到低）：
1. 环境变量
2. `application-{profile}.yml`（如 `application-prod.yml`）
3. `application.yml`

**重要**：所有敏感信息（数据库密码、Redis 密码、OSS 密钥等）必须通过环境变量注入，不允许在配置文件中硬编码。

## 环境变量清单

### 数据库配置

| 环境变量名 | 配置路径 | 说明 | 是否必填 |
|----------|---------|------|---------|
| `DB_URL` | `spring.datasource.url` | 数据库连接 URL | 是 |
| `DB_USERNAME` | `spring.datasource.username` | 数据库用户名 | 是 |
| `DB_PASSWORD` | `spring.datasource.password` | 数据库密码 | 是 |

**示例**：
```bash
DB_URL=jdbc:mysql://mysql.example.com:3306/bluecone?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=bluecone_user
DB_PASSWORD=your_strong_password
```

### Redis 配置

| 环境变量名 | 配置路径 | 说明 | 是否必填 |
|----------|---------|------|---------|
| `REDIS_HOST` | `spring.data.redis.host` | Redis 主机地址 | 是 |
| `REDIS_PORT` | `spring.data.redis.port` | Redis 端口 | 是 |
| `REDIS_DATABASE` | `spring.data.redis.database` | Redis 数据库编号 | 是 |
| `REDIS_USERNAME` | `spring.data.redis.username` | Redis 用户名（可选） | 否 |
| `REDIS_PASSWORD` | `spring.data.redis.password` | Redis 密码 | 是 |

**示例**：
```bash
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_DATABASE=0
REDIS_USERNAME=bluecone_user
REDIS_PASSWORD=your_strong_password
```

### OSS 对象存储配置

| 环境变量名 | 配置路径 | 说明 | 是否必填 |
|----------|---------|------|---------|
| `OSS_ENDPOINT` | `bluecone.storage.aliyun.endpoint` | OSS 端点地址 | 是 |
| `OSS_ACCESS_KEY_ID` | `bluecone.storage.aliyun.access-key-id` | OSS Access Key ID | 是 |
| `OSS_ACCESS_KEY_SECRET` | `bluecone.storage.aliyun.access-key-secret` | OSS Access Key Secret | 是 |
| `OSS_BUCKET` | `bluecone.storage.aliyun.default-bucket` | OSS 默认 Bucket | 是 |
| `OSS_CDN_DOMAIN` | `bluecone.storage.aliyun.cdn-domain` | OSS CDN 域名 | 是 |
| `OSS_PUBLIC_DOMAIN` | `bluecone.storage.aliyun.public-domain` | OSS 公共域名 | 是 |

**示例**：
```bash
OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
OSS_ACCESS_KEY_ID=your_access_key_id
OSS_ACCESS_KEY_SECRET=your_access_key_secret
OSS_BUCKET=bluecone-prod
OSS_CDN_DOMAIN=https://cdn.bluecone.com
OSS_PUBLIC_DOMAIN=https://img.bluecone.com
```

### 安全配置

| 环境变量名 | 配置路径 | 说明 | 是否必填 |
|----------|---------|------|---------|
| `JWT_SECRET` | `bluecone.security.token.secret` | JWT 签名密钥 | 是 |
| `GATEWAY_SIGNATURE_SECRET` | `bluecone.gateway.signature-secret` | Gateway 签名密钥 | 是 |

**示例**：
```bash
JWT_SECRET=your_jwt_secret_at_least_32_characters_long
GATEWAY_SIGNATURE_SECRET=your_gateway_secret_at_least_32_characters_long
```

**安全要求**：
- JWT Secret 和 Gateway Secret 必须使用足够长度的随机字符串（建议 32 字符以上）
- 生产环境必须定期轮换密钥
- 密钥泄露后必须立即轮换

## 微信云托管配置方式

### 方式一：通过控制台配置

1. 登录微信云托管控制台
2. 进入「环境管理」->「环境变量」
3. 添加上述所有环境变量
4. 保存并重启服务

### 方式二：通过配置文件配置

在微信云托管项目中，可以创建 `cloudbaserc.json` 或使用环境变量配置文件：

```json
{
  "envId": "your-env-id",
  "version": "2.0",
  "envVariables": {
    "DB_URL": "jdbc:mysql://mysql.example.com:3306/bluecone?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
    "DB_USERNAME": "bluecone_user",
    "DB_PASSWORD": "your_strong_password",
    "REDIS_HOST": "redis.example.com",
    "REDIS_PORT": "6379",
    "REDIS_DATABASE": "0",
    "REDIS_PASSWORD": "your_strong_password",
    "OSS_ENDPOINT": "https://oss-cn-hangzhou.aliyuncs.com",
    "OSS_ACCESS_KEY_ID": "your_access_key_id",
    "OSS_ACCESS_KEY_SECRET": "your_access_key_secret",
    "OSS_BUCKET": "bluecone-prod",
    "OSS_CDN_DOMAIN": "https://cdn.bluecone.com",
    "OSS_PUBLIC_DOMAIN": "https://img.bluecone.com",
    "JWT_SECRET": "your_jwt_secret_at_least_32_characters_long",
    "GATEWAY_SIGNATURE_SECRET": "your_gateway_secret_at_least_32_characters_long"
  }
}
```

### 方式三：通过 CI/CD 配置

在 CI/CD 流程中，可以通过脚本设置环境变量：

```bash
# 设置环境变量
export DB_URL="jdbc:mysql://mysql.example.com:3306/bluecone?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
export DB_USERNAME="bluecone_user"
export DB_PASSWORD="your_strong_password"
# ... 其他环境变量

# 部署到微信云托管
tcb deploy
```

## 本地开发配置

### 步骤 1：复制示例文件

```bash
cd app-application/src/main/resources
cp application-local.example.yml application-local.yml
```

### 步骤 2：编辑本地配置

编辑 `application-local.yml`，填入您的本地配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your_local_password
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: your_redis_password
```

### 步骤 3：启动应用

使用 `local` profile 启动应用：

```bash
# Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或使用 JAR
java -jar target/bluecone-app.jar --spring.profiles.active=local
```

## 各环境配置差异

### Local 环境

- **Flyway 配置**：
  - `validate-on-migrate: false` - 允许本地容错
  - `ignore-migration-patterns: "*:missing"` - 允许忽略缺失迁移
  - **原因**：本地开发可能使用不完整的数据库状态，需要容错

- **缓存配置**：
  - 可以禁用 L2 缓存（Redis）和一致性总线
  - **原因**：如果本地没有启动 Redis，可以禁用以避免连接错误

- **安全配置**：
  - 可以使用默认开发密钥
  - **原因**：仅用于本地开发，生产环境必须配置

### Dev 环境

- **Flyway 配置**：
  - `validate-on-migrate: true` - 强制校验迁移脚本
  - **不设置** `ignore-migration-patterns` - 禁止忽略缺失迁移
  - **原因**：开发环境必须保证迁移脚本的完整性

- **所有敏感配置**：
  - 必须通过环境变量注入
  - **原因**：确保配置安全，避免硬编码

### Prod 环境

- **Flyway 配置**：
  - `validate-on-migrate: true` - 强制校验迁移脚本
  - **不设置** `ignore-migration-patterns` - 禁止忽略缺失迁移
  - **原因**：生产环境必须保证迁移脚本的完整性和可重复性

- **所有敏感配置**：
  - 必须通过环境变量注入
  - **原因**：确保配置安全，避免硬编码

- **连接池配置**：
  - 使用较大的连接池（`maximum-pool-size: 20`）
  - **原因**：生产环境需要应对高并发

- **日志级别**：
  - 使用 WARN 级别（减少日志量）
  - **原因**：生产环境需要减少日志输出，提高性能

## Flyway 策略说明

### Local 环境策略

**配置**：
- `validate-on-migrate: false`
- `ignore-migration-patterns: "*:missing"`

**原因**：
- 本地开发可能使用不完整的数据库状态
- 需要容错以避免启动失败
- 允许快速迭代和测试

**风险**：
- 数据库状态可能与代码不完全一致
- 仅用于本地开发，不能用于生产环境

### Dev/Prod 环境策略

**配置**：
- `validate-on-migrate: true`
- **不设置** `ignore-migration-patterns`

**原因**：
- 线上环境必须保证迁移脚本的完整性
- 确保数据库状态与代码一致
- 支持回滚操作

**风险**：
- 如果数据库状态与迁移脚本不一致，应用启动将失败
- 这是预期的安全行为，确保数据库状态的一致性

## 安全最佳实践

1. **密钥管理**：
   - 所有密钥必须通过环境变量注入
   - 生产环境必须使用强密码和密钥
   - 定期轮换密钥

2. **最小权限原则**：
   - 数据库用户使用最小权限
   - OSS Access Key 使用最小权限
   - 避免使用 root 账号

3. **配置验证**：
   - 应用启动时验证所有必填配置
   - 如果环境变量未设置，应用启动将失败（这是预期的安全行为）

4. **配置分离**：
   - 本地配置文件（`application-local.yml`）不提交到版本库
   - 生产环境配置仅包含占位符，不包含真实密钥

## 故障排查

### 问题：应用启动失败，提示配置缺失

**原因**：环境变量未设置或配置错误

**解决方案**：
1. 检查环境变量是否已正确设置
2. 检查环境变量名称是否正确
3. 检查配置文件中的环境变量引用是否正确

### 问题：Flyway 迁移失败

**原因**：数据库状态与迁移脚本不一致

**解决方案**：
1. 检查数据库状态是否与迁移脚本一致
2. 如果是本地开发，可以使用 `validate-on-migrate: false` 和 `ignore-migration-patterns: "*:missing"`
3. 如果是生产环境，必须修复数据库状态或迁移脚本

### 问题：Redis 连接失败

**原因**：Redis 配置错误或 Redis 服务未启动

**解决方案**：
1. 检查 Redis 配置是否正确
2. 检查 Redis 服务是否已启动
3. 如果是本地开发，可以禁用 Redis 相关功能

## 参考文档

- [Spring Boot 配置文档](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Flyway 配置文档](https://flywaydb.org/documentation/configuration/parameters/)
- [微信云托管文档](https://cloud.tencent.com/document/product/876)
