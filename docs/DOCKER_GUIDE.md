# Docker 构建和部署指南

本指南介绍如何使用优化后的 Dockerfile 在微信云托管环境中构建和运行 BlueCone 应用。

## 📋 目录

- [优化特性](#优化特性)
- [快速开始](#快速开始)
- [构建说明](#构建说明)
- [运行配置](#运行配置)
- [微信云托管部署](#微信云托管部署)
- [故障排查](#故障排查)

## ✨ 优化特性

### 1. 完整的模块支持

Dockerfile 现在支持项目中的所有 31 个子模块：

**平台模块 (Platform Modules)**
- app-platform-bom
- app-id-api, app-id
- app-core
- app-infra
- app-ops

**平台启动器 (Platform Starters)**
- app-platform-starter
- app-platform-starter-ops
- app-platform-archkit
- app-platform-codegen

**资源模块 (Resource Modules)**
- app-resource-api, app-resource
- app-security

**业务模块 (Business Modules)**
- app-tenant
- app-store
- app-product
- app-member-api, app-member
- app-promo-api, app-promo
- app-wallet-api, app-wallet
- app-pricing-api, app-pricing
- app-billing-api, app-billing
- app-notify-api, app-notify
- app-growth-api, app-growth
- app-campaign-api, app-campaign
- app-order
- app-payment
- app-inventory
- app-wechat

**应用模块 (Application Module)**
- app-application

### 2. 构建优化

- ✅ **多阶段构建**: 分离构建和运行环境，减小最终镜像大小
- ✅ **Docker 层缓存**: 优先拷贝 pom.xml 文件，充分利用 Docker 缓存
- ✅ **多线程构建**: 使用 `-T 1C` 参数启用多线程 Maven 构建
- ✅ **依赖预下载**: 提前下载依赖，加速重复构建
- ✅ **腾讯镜像加速**: 使用腾讯 APK 镜像，加速在微信云托管的构建

### 3. 运行时优化

- ✅ **JVM 容器感知**: 自动适配容器内存限制
- ✅ **G1GC 垃圾收集器**: 优化垃圾回收性能
- ✅ **内存自适应**: 根据容器内存自动调整堆大小
- ✅ **时区设置**: 默认使用 Asia/Shanghai 时区
- ✅ **OOM 诊断**: 自动生成堆转储文件，便于问题排查

### 4. 环境配置

- ✅ **Spring Profile 支持**: 默认使用 `local` profile，可通过环境变量覆盖
- ✅ **端口灵活配置**: 兼容微信云托管的 PORT 环境变量
- ✅ **日志目录**: 预创建日志目录，支持持久化日志

## 🚀 快速开始

### 使用构建脚本（推荐）

```bash
# 构建 local 环境镜像（默认）
./docker-build.sh

# 构建指定环境镜像
./docker-build.sh dev
./docker-build.sh test
./docker-build.sh prod
```

### 手动构建

```bash
# 构建镜像
docker build -t bluecone-app:latest .

# 运行容器
docker run -p 8080:80 \
  -e SPRING_PROFILES_ACTIVE=local \
  bluecone-app:latest
```

## 🔧 构建说明

### 构建参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring 激活的配置文件 | `local` |
| `PORT` | 应用监听端口 | `80` |
| `JAVA_OPTS` | JVM 参数 | 见下文 |

### 默认 JVM 参数

```bash
-XX:+UseG1GC                      # 使用 G1 垃圾收集器
-XX:MaxRAMPercentage=75.0         # 最大堆内存占容器内存的 75%
-XX:InitialRAMPercentage=50.0    # 初始堆内存占容器内存的 50%
-XX:+UseContainerSupport          # 启用容器感知
-XX:MaxGCPauseMillis=200          # GC 最大暂停时间 200ms
-XX:+HeapDumpOnOutOfMemoryError   # OOM 时生成堆转储
-XX:HeapDumpPath=/app/logs        # 堆转储文件路径
-Djava.security.egd=file:/dev/./urandom  # 加速随机数生成
-Dfile.encoding=UTF-8             # 文件编码
-Duser.timezone=Asia/Shanghai     # 时区设置
```

### 覆盖 JVM 参数

```bash
docker run -p 8080:80 \
  -e JAVA_OPTS="-Xmx2g -Xms1g" \
  bluecone-app:latest
```

## ⚙️ 运行配置

### 基本运行

```bash
docker run -p 8080:80 bluecone-app:latest
```

### 指定 Spring Profile

```bash
# 使用 dev 环境配置
docker run -p 8080:80 \
  -e SPRING_PROFILES_ACTIVE=dev \
  bluecone-app:latest

# 使用 prod 环境配置
docker run -p 8080:80 \
  -e SPRING_PROFILES_ACTIVE=prod \
  bluecone-app:latest
```

### 挂载日志目录

```bash
docker run -p 8080:80 \
  -v $(pwd)/logs:/app/logs \
  bluecone-app:latest
```

### 传递额外的 Spring 参数

```bash
docker run -p 8080:80 \
  -e SPRING_PROFILES_ACTIVE=local \
  bluecone-app:latest \
  --spring.datasource.url=jdbc:mysql://localhost:3306/bluecone \
  --spring.datasource.username=root \
  --spring.datasource.password=password
```

### 使用 Docker Compose

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  bluecone-app:
    image: bluecone-app:latest
    ports:
      - "8080:80"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - PORT=80
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
```

运行：

```bash
docker-compose up -d
```

## ☁️ 微信云托管部署

### 1. 准备工作

确保已安装微信云托管 CLI：

```bash
# 安装 CLI
npm install -g @cloudbase/cli

# 登录
tcb login
```

### 2. 构建镜像

```bash
# 构建生产环境镜像
./docker-build.sh prod
```

### 3. 推送到镜像仓库

```bash
# 标记镜像
docker tag bluecone-app:latest \
  ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest

# 登录腾讯云容器镜像服务
docker login ccr.ccs.tencentyun.com

# 推送镜像
docker push ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest
```

### 4. 部署到云托管

在微信云托管控制台：

1. 创建新版本
2. 选择镜像：`ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest`
3. 设置环境变量：
   - `SPRING_PROFILES_ACTIVE=prod`
   - 其他必要的配置（数据库连接等）
4. 配置资源规格（建议至少 1 核 2GB）
5. 部署

### 5. 环境变量配置示例

在云托管控制台配置以下环境变量：

```
# Spring 配置
SPRING_PROFILES_ACTIVE=prod

# 数据库配置
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/bluecone
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password

# 微信配置
WECHAT_APP_ID=your-app-id
WECHAT_APP_SECRET=your-app-secret

# JVM 配置（可选）
JAVA_OPTS=-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0
```

## 🔍 故障排查

### 构建失败

**问题**: Maven 依赖下载失败

```bash
# 解决方案：使用国内镜像
# 在 Dockerfile 中添加 Maven settings.xml
```

**问题**: 内存不足

```bash
# 解决方案：增加 Docker 构建内存限制
docker build --memory=4g -t bluecone-app:latest .
```

### 运行时问题

**问题**: 容器启动后立即退出

```bash
# 查看日志
docker logs <container-id>

# 检查环境变量
docker inspect <container-id>
```

**问题**: 应用无法连接数据库

```bash
# 确保数据库连接配置正确
# 检查网络连通性
docker run --rm -it bluecone-app:latest sh
ping your-db-host
```

**问题**: OOM (Out of Memory)

```bash
# 增加容器内存限制
docker run -m 2g -p 8080:80 bluecone-app:latest

# 或调整 JVM 参数
docker run -p 8080:80 \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60.0" \
  bluecone-app:latest
```

### 性能优化

**查看容器资源使用情况**

```bash
docker stats <container-id>
```

**查看应用日志**

```bash
# 实时查看日志
docker logs -f <container-id>

# 查看最近 100 行日志
docker logs --tail 100 <container-id>
```

**进入容器调试**

```bash
docker exec -it <container-id> sh
```

## 📊 镜像大小优化

当前优化后的镜像大小约为：

- 构建镜像（build stage）: ~1.5GB
- 运行镜像（final stage）: ~300-400MB

进一步优化建议：

1. 使用 `jlink` 创建自定义 JRE
2. 使用 `distroless` 基础镜像
3. 启用 Maven 依赖缓存

## 🔗 相关资源

- [微信云托管文档](https://cloud.weixin.qq.com/cloudrun)
- [Docker 最佳实践](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker 指南](https://spring.io/guides/topicals/spring-boot-docker/)

## 📝 更新日志

### 2025-12-23
- ✅ 支持全部 31 个子模块
- ✅ 优化构建性能（多线程构建）
- ✅ 优化运行时 JVM 参数
- ✅ 添加 Spring Profile 支持（默认 local）
- ✅ 添加 .dockerignore 文件
- ✅ 添加构建脚本和文档

