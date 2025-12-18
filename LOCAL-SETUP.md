# BlueCone Application - 本地开发环境配置指南

## 📋 环境变量清单

### 必需环境变量（生产环境）

| 环境变量 | 说明 | 示例值 |
|---------|------|--------|
| `DB_URL` | MySQL 数据库连接 URL | `jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `your_password` |
| `REDIS_HOST` | Redis 主机地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_DATABASE` | Redis 数据库编号 | `0` |
| `REDIS_USERNAME` | Redis 用户名（可选） |  |
| `REDIS_PASSWORD` | Redis 密码（可选） |  |
| `OSS_ENDPOINT` | 阿里云 OSS 端点 | `https://oss-cn-hangzhou.aliyuncs.com` |
| `OSS_ACCESS_KEY_ID` | OSS 访问密钥 ID | `your_access_key_id` |
| `OSS_ACCESS_KEY_SECRET` | OSS 访问密钥 Secret | `your_access_key_secret` |
| `OSS_BUCKET` | OSS 存储桶名称 | `bluecone` |
| `OSS_CDN_DOMAIN` | OSS CDN 域名 | `https://cdn.bluecone.com` |
| `OSS_PUBLIC_DOMAIN` | OSS 公共域名 | `https://img.bluecone.com` |

## 🚀 本地启动方式

### 方式 1：使用 application-local.yml（推荐本地开发）

1. **复制配置模板**
   ```bash
   cd app-application/src/main/resources
   cp application-local.yml.template application-local.yml
   ```

2. **编辑 application-local.yml**
   填入您的本地数据库、Redis、OSS 等配置信息

3. **启动应用**
   ```bash
   mvn -pl app-application -am spring-boot:run -Dspring-boot.run.profiles=local
   ```

### 方式 2：使用环境变量（推荐生产环境）

```bash
# 设置环境变量
export DB_URL=jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_DATABASE=0
export REDIS_PASSWORD=
export OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
export OSS_ACCESS_KEY_ID=your_access_key_id
export OSS_ACCESS_KEY_SECRET=your_access_key_secret
export OSS_BUCKET=bluecone

# 启动应用
mvn -pl app-application -am spring-boot:run
```

### 方式 3：Docker Compose 快速启动（可选）

创建 `docker-compose.yml`：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.3.0
    environment:
      MYSQL_ROOT_PASSWORD: bluecone
      MYSQL_DATABASE: bluecone
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  mysql_data:
  redis_data:
```

启动：
```bash
docker-compose up -d
```

## 🔍 验证配置

### 检查环境变量
```bash
# 检查所有环境变量
env | grep -E "(DB_|REDIS_|OSS_)"
```

### 检查数据库连接
```bash
mysql -h localhost -u root -p bluecone
```

### 检查 Redis 连接
```bash
redis-cli -h localhost -p 6379 ping
```

## ⚠️ 注意事项

1. **敏感信息保护**
   - `application-local.yml` 已加入 `.gitignore`，不会被提交到仓库
   - 生产环境必须使用环境变量，禁止在配置文件中硬编码敏感信息

2. **Flyway 迁移**
   - 首次启动会自动执行数据库迁移脚本
   - 确保数据库用户有 CREATE TABLE、ALTER TABLE 权限
   - 迁移脚本位于 `app-infra/src/main/resources/db/migration/`

3. **配置文件优先级**
   - `application-local.yml` > `application.yml` > 环境变量
   - 使用 `spring.profiles.active=local` 激活本地配置

## 📚 相关文档

- [QUICK-START.md](QUICK-START.md) - 快速启动指南
- [application-example.yml](app-application/src/main/resources/application-example.yml) - 配置结构示例

---

**最后更新**: 2025-12-16

