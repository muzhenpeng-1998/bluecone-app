# BlueCone App - Docker 部署方案

## 📦 概述

本项目提供了完整的 Docker 化部署方案，专门针对微信云托管环境进行了优化，支持项目中所有 31 个子模块的构建和运行。

## 🎯 核心特性

### ✅ 完整的模块支持
- 支持所有 31 个子模块（包括平台模块、业务模块、API 模块等）
- 按照正确的依赖顺序构建，确保构建成功

### ✅ 构建优化
- **多阶段构建**: 分离构建和运行环境，最终镜像仅 300-400MB
- **Docker 层缓存**: 优先拷贝 pom.xml，充分利用缓存机制
- **多线程构建**: 使用 Maven `-T 1C` 参数，加速构建过程
- **国内镜像加速**: 使用阿里云和腾讯云镜像，大幅提升构建速度

### ✅ 运行时优化
- **JVM 容器感知**: 自动适配容器内存限制
- **G1GC 垃圾收集器**: 优化垃圾回收性能
- **内存自适应**: 根据容器内存自动调整堆大小（默认 75%）
- **时区配置**: 默认使用 Asia/Shanghai 时区
- **OOM 诊断**: 自动生成堆转储文件

### ✅ 环境配置
- **Spring Profile 支持**: 默认 `local`，支持 `dev`、`test`、`prod`
- **端口灵活配置**: 兼容微信云托管的 PORT 环境变量
- **日志持久化**: 预创建日志目录，支持日志挂载

## 📁 文件说明

```
.
├── Dockerfile              # 主 Dockerfile（多阶段构建）
├── .dockerignore          # Docker 忽略文件配置
├── docker-compose.yml     # Docker Compose 配置
├── docker-build.sh        # 构建脚本
├── .mvn/
│   └── settings.xml       # Maven 配置（国内镜像加速）
├── DOCKER_GUIDE.md        # 详细使用指南
└── DOCKER_README.md       # 本文件
```

## 🚀 快速开始

### 方式一：使用构建脚本（推荐）

```bash
# 构建 local 环境镜像
./docker-build.sh

# 构建指定环境镜像
./docker-build.sh dev
./docker-build.sh test
./docker-build.sh prod
```

### 方式二：使用 Docker 命令

```bash
# 构建镜像
docker build -t bluecone-app:latest .

# 运行容器
docker run -p 8080:80 \
  -e SPRING_PROFILES_ACTIVE=local \
  bluecone-app:latest
```

### 方式三：使用 Docker Compose

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 🔧 环境变量配置

### 必需配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | Spring 激活的配置文件 | `local` | `dev`, `test`, `prod` |

### 可选配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `PORT` | 应用监听端口 | `80` |
| `JAVA_OPTS` | JVM 参数 | 见下文 |

### 数据库配置（根据实际情况设置）

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://your-host:3306/bluecone
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password
```

## 📊 默认 JVM 配置

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

## 🌐 微信云托管部署

### 1. 构建镜像

```bash
./docker-build.sh prod
```

### 2. 推送到腾讯云容器镜像服务

```bash
# 登录
docker login ccr.ccs.tencentyun.com

# 标记镜像
docker tag bluecone-app:latest \
  ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest

# 推送
docker push ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest
```

### 3. 在云托管控制台部署

1. 进入微信云托管控制台
2. 创建新版本
3. 选择镜像：`ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest`
4. 配置环境变量（见下文）
5. 设置资源规格（建议至少 1 核 2GB）
6. 部署

### 4. 环境变量配置示例

```bash
# Spring 配置
SPRING_PROFILES_ACTIVE=prod

# 数据库配置
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/bluecone
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password

# 微信配置
WECHAT_APP_ID=your-app-id
WECHAT_APP_SECRET=your-app-secret
```

## 🔍 常用命令

### 构建相关

```bash
# 构建镜像（不使用缓存）
docker build --no-cache -t bluecone-app:latest .

# 构建镜像（指定内存限制）
docker build --memory=4g -t bluecone-app:latest .

# 查看镜像大小
docker images bluecone-app
```

### 运行相关

```bash
# 运行容器（后台运行）
docker run -d -p 8080:80 \
  --name bluecone-app \
  -e SPRING_PROFILES_ACTIVE=local \
  bluecone-app:latest

# 运行容器（挂载日志目录）
docker run -d -p 8080:80 \
  -v $(pwd)/logs:/app/logs \
  bluecone-app:latest

# 运行容器（设置资源限制）
docker run -d -p 8080:80 \
  --memory=2g \
  --cpus=2 \
  bluecone-app:latest
```

### 调试相关

```bash
# 查看容器日志
docker logs -f bluecone-app

# 查看最近 100 行日志
docker logs --tail 100 bluecone-app

# 进入容器
docker exec -it bluecone-app sh

# 查看容器资源使用情况
docker stats bluecone-app

# 查看容器详细信息
docker inspect bluecone-app
```

### 清理相关

```bash
# 停止容器
docker stop bluecone-app

# 删除容器
docker rm bluecone-app

# 删除镜像
docker rmi bluecone-app:latest

# 清理未使用的镜像
docker image prune -a

# 清理所有未使用的资源
docker system prune -a
```

## 📈 性能优化建议

### 1. 构建优化

- ✅ 已启用多线程构建（`-T 1C`）
- ✅ 已配置国内镜像加速
- ✅ 已优化 Docker 层缓存
- 💡 建议：使用 BuildKit 加速构建

```bash
# 启用 BuildKit
export DOCKER_BUILDKIT=1
docker build -t bluecone-app:latest .
```

### 2. 运行时优化

- ✅ 已启用 G1GC 垃圾收集器
- ✅ 已配置容器感知
- ✅ 已优化内存配置
- 💡 建议：根据实际负载调整 JVM 参数

```bash
# 示例：调整堆内存比例
docker run -p 8080:80 \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=40.0" \
  bluecone-app:latest
```

### 3. 镜像大小优化

当前镜像大小：~300-400MB

进一步优化方案：
- 使用 `jlink` 创建自定义 JRE
- 使用 `distroless` 基础镜像
- 启用 Maven 依赖缓存

## 🐛 故障排查

### 构建失败

**问题**: Maven 依赖下载失败

```bash
# 解决方案：检查网络连接，或使用 VPN
# 已配置阿里云镜像，通常不会出现此问题
```

**问题**: 内存不足

```bash
# 解决方案：增加 Docker 构建内存限制
docker build --memory=4g -t bluecone-app:latest .
```

### 运行失败

**问题**: 容器启动后立即退出

```bash
# 查看日志
docker logs bluecone-app

# 常见原因：
# 1. 数据库连接失败
# 2. 配置文件错误
# 3. 端口被占用
```

**问题**: 应用无法连接数据库

```bash
# 检查数据库连接配置
docker exec -it bluecone-app sh
ping your-db-host

# 检查环境变量
docker inspect bluecone-app | grep -A 20 Env
```

**问题**: OOM (Out of Memory)

```bash
# 解决方案 1：增加容器内存限制
docker run -m 2g -p 8080:80 bluecone-app:latest

# 解决方案 2：调整 JVM 参数
docker run -p 8080:80 \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60.0" \
  bluecone-app:latest
```

## 📚 相关文档

- [DOCKER_GUIDE.md](DOCKER_GUIDE.md) - 详细使用指南
- [微信云托管文档](https://cloud.weixin.qq.com/cloudrun)
- [Docker 最佳实践](https://docs.docker.com/develop/dev-best-practices/)

## 🔄 更新日志

### 2025-12-23
- ✅ 支持全部 31 个子模块
- ✅ 优化构建性能（多线程构建）
- ✅ 添加国内镜像加速配置
- ✅ 优化运行时 JVM 参数
- ✅ 添加 Spring Profile 支持（默认 local）
- ✅ 添加 .dockerignore 文件
- ✅ 添加构建脚本和文档
- ✅ 添加 Docker Compose 配置
- ✅ 添加健康检查配置

## 💡 最佳实践

1. **开发环境**: 使用 `docker-compose.yml` 快速启动本地开发环境
2. **测试环境**: 使用 `./docker-build.sh test` 构建测试镜像
3. **生产环境**: 使用 `./docker-build.sh prod` 构建生产镜像，并推送到云端
4. **日志管理**: 始终挂载 `/app/logs` 目录，便于日志收集和分析
5. **资源限制**: 在生产环境中设置合理的 CPU 和内存限制
6. **健康检查**: 启用健康检查，确保服务可用性

## 🤝 贡献

如有问题或建议，欢迎提交 Issue 或 Pull Request。

## 📄 许可证

[根据项目实际情况填写]

