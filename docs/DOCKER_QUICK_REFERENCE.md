# Docker 快速参考卡

## 🚀 快速启动

```bash
# 方式 1: 使用构建脚本（推荐）
./docker-build.sh

# 方式 2: 使用 Docker Compose
docker-compose up -d

# 方式 3: 手动构建和运行
docker build -t bluecone-app:latest .
docker run -p 8080:80 bluecone-app:latest
```

## 📦 构建命令

```bash
# 基础构建
docker build -t bluecone-app:latest .

# 不使用缓存构建
docker build --no-cache -t bluecone-app:latest .

# 指定内存限制构建
docker build --memory=4g -t bluecone-app:latest .

# 使用 BuildKit 加速构建
DOCKER_BUILDKIT=1 docker build -t bluecone-app:latest .

# 构建指定环境
./docker-build.sh local   # 本地环境
./docker-build.sh dev     # 开发环境
./docker-build.sh test    # 测试环境
./docker-build.sh prod    # 生产环境
```

## 🏃 运行命令

```bash
# 基础运行
docker run -p 8080:80 bluecone-app:latest

# 后台运行
docker run -d -p 8080:80 --name bluecone-app bluecone-app:latest

# 指定 Spring Profile
docker run -p 8080:80 -e SPRING_PROFILES_ACTIVE=dev bluecone-app:latest

# 挂载日志目录
docker run -p 8080:80 -v $(pwd)/logs:/app/logs bluecone-app:latest

# 设置资源限制
docker run -p 8080:80 --memory=2g --cpus=2 bluecone-app:latest

# 自定义 JVM 参数
docker run -p 8080:80 \
  -e JAVA_OPTS="-Xmx2g -Xms1g" \
  bluecone-app:latest

# 完整示例（生产环境）
docker run -d \
  --name bluecone-app \
  -p 8080:80 \
  --memory=2g \
  --cpus=2 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/bluecone \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -v $(pwd)/logs:/app/logs \
  --restart unless-stopped \
  bluecone-app:latest
```

## 🔍 调试命令

```bash
# 查看日志（实时）
docker logs -f bluecone-app

# 查看最近 100 行日志
docker logs --tail 100 bluecone-app

# 查看日志（带时间戳）
docker logs -t bluecone-app

# 进入容器
docker exec -it bluecone-app sh

# 查看容器资源使用
docker stats bluecone-app

# 查看容器详细信息
docker inspect bluecone-app

# 查看容器环境变量
docker inspect bluecone-app | grep -A 20 Env

# 查看容器进程
docker top bluecone-app

# 查看容器端口映射
docker port bluecone-app
```

## 🛠️ 管理命令

```bash
# 停止容器
docker stop bluecone-app

# 启动容器
docker start bluecone-app

# 重启容器
docker restart bluecone-app

# 删除容器
docker rm bluecone-app

# 强制删除容器
docker rm -f bluecone-app

# 查看所有容器
docker ps -a

# 查看运行中的容器
docker ps

# 查看镜像列表
docker images

# 删除镜像
docker rmi bluecone-app:latest

# 删除未使用的镜像
docker image prune

# 删除所有未使用的资源
docker system prune -a
```

## 🐳 Docker Compose 命令

```bash
# 启动服务
docker-compose up

# 后台启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 停止并删除卷
docker-compose down -v

# 查看日志
docker-compose logs

# 实时查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f bluecone-app

# 重启服务
docker-compose restart

# 重新构建并启动
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 进入服务容器
docker-compose exec bluecone-app sh

# 扩展服务实例
docker-compose up -d --scale bluecone-app=3
```

## 🌐 镜像推送（微信云托管）

```bash
# 1. 登录腾讯云容器镜像服务
docker login ccr.ccs.tencentyun.com

# 2. 标记镜像
docker tag bluecone-app:latest \
  ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest

# 3. 推送镜像
docker push ccr.ccs.tencentyun.com/your-namespace/bluecone-app:latest

# 4. 推送多个标签
docker tag bluecone-app:latest \
  ccr.ccs.tencentyun.com/your-namespace/bluecone-app:v1.0.0
docker push ccr.ccs.tencentyun.com/your-namespace/bluecone-app:v1.0.0
```

## 🔧 环境变量

### 必需变量

```bash
SPRING_PROFILES_ACTIVE=local  # Spring 配置文件（local/dev/test/prod）
```

### 可选变量

```bash
PORT=80                       # 应用端口
JAVA_OPTS="-Xmx2g"           # JVM 参数
```

### 数据库配置

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/bluecone
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=password
```

### 微信配置

```bash
WECHAT_APP_ID=your-app-id
WECHAT_APP_SECRET=your-app-secret
```

## 🐛 常见问题

### 构建失败

```bash
# 清理缓存重新构建
docker build --no-cache -t bluecone-app:latest .

# 增加构建内存
docker build --memory=4g -t bluecone-app:latest .
```

### 容器无法启动

```bash
# 查看详细日志
docker logs bluecone-app

# 检查环境变量
docker inspect bluecone-app | grep -A 20 Env
```

### 端口被占用

```bash
# 查看端口占用
lsof -i :8080

# 使用其他端口
docker run -p 9090:80 bluecone-app:latest
```

### 内存不足

```bash
# 增加容器内存限制
docker run -m 2g -p 8080:80 bluecone-app:latest

# 调整 JVM 参数
docker run -p 8080:80 \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=60.0" \
  bluecone-app:latest
```

### 数据库连接失败

```bash
# 进入容器测试连接
docker exec -it bluecone-app sh
ping your-db-host

# 检查数据库配置
docker inspect bluecone-app | grep DATASOURCE
```

## 📊 性能监控

```bash
# 查看容器资源使用
docker stats bluecone-app

# 查看所有容器资源使用
docker stats

# 导出容器统计信息
docker stats --no-stream > stats.txt

# 查看容器进程
docker top bluecone-app
```

## 🧹 清理命令

```bash
# 停止所有容器
docker stop $(docker ps -aq)

# 删除所有容器
docker rm $(docker ps -aq)

# 删除所有镜像
docker rmi $(docker images -q)

# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 清理未使用的网络
docker network prune

# 清理所有未使用的资源
docker system prune -a --volumes

# 查看 Docker 磁盘使用情况
docker system df
```

## 💡 最佳实践

```bash
# 1. 使用构建脚本
./docker-build.sh prod

# 2. 使用 Docker Compose 管理多服务
docker-compose up -d

# 3. 始终挂载日志目录
docker run -v $(pwd)/logs:/app/logs bluecone-app:latest

# 4. 设置资源限制
docker run --memory=2g --cpus=2 bluecone-app:latest

# 5. 使用健康检查
docker run --health-cmd="wget -q --spider http://localhost:80/actuator/health" \
  bluecone-app:latest

# 6. 使用重启策略
docker run --restart unless-stopped bluecone-app:latest

# 7. 使用网络隔离
docker network create bluecone-network
docker run --network bluecone-network bluecone-app:latest
```

## 📚 更多信息

- 详细指南: [DOCKER_GUIDE.md](DOCKER_GUIDE.md)
- 完整文档: [DOCKER_README.md](DOCKER_README.md)
- 微信云托管: https://cloud.weixin.qq.com/cloudrun

