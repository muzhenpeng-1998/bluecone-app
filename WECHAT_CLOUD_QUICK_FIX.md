# 微信云托管快速修复指南

## 🚨 健康检查失败问题

### 错误信息
```
Liveness probe failed: dial tcp 10.19.11.147:80: connect: connection refused
Readiness probe failed: dial tcp 10.19.11.147:80: connect: connection refused
```

### ⚡ 快速修复方案

#### 方案 A：优化应用启动速度（推荐，无需修改控制台）

如果微信云托管控制台**没有健康检查配置选项**，使用此方案。

**步骤 1**: 设置环境变量

在微信云托管控制台添加：

```bash
SPRING_PROFILES_ACTIVE=prod-fast
```

**步骤 2**: 重新部署

启动时间将从 30 秒缩短到 **15-20 秒**，大幅提高启动成功率。

详细说明请查看：`FAST_STARTUP_GUIDE.md`

---

#### 方案 B：修改健康检查配置（如果控制台支持）

1. 登录 [微信云托管控制台](https://cloud.weixin.qq.com/cloudrun)
2. 进入服务 → 版本管理 → 编辑版本
3. 找到 **健康检查配置**
4. 修改以下参数：

```
Liveness Probe（存活探针）:
  ✅ 检查路径: /internal/actuator/health/liveness
  ✅ 检查端口: 80
  ✅ 初始延迟: 60 秒  ⬅️ 关键！从默认的 10-30 秒改为 60 秒
  ✅ 检查间隔: 10 秒
  ✅ 超时时间: 5 秒
  ✅ 失败阈值: 3 次

Readiness Probe（就绪探针）:
  ✅ 检查路径: /internal/actuator/health/readiness
  ✅ 检查端口: 80
  ✅ 初始延迟: 30 秒  ⬅️ 关键！从默认的 5-10 秒改为 30 秒
  ✅ 检查间隔: 5 秒
  ✅ 超时时间: 3 秒
  ✅ 失败阈值: 3 次
```

5. 保存并重新部署

### 📊 验证修复

部署后查看日志，应该看到：

```json
{"message":"Started Application in XX.XXX seconds","level":"INFO"}
```

不再出现 `connection refused` 错误。

---

## 🔧 MyBatis 重复扫描警告

### 错误信息
```
Skipping MapperFactoryBean with name 'memberMapper' ... Bean already defined with the same name!
No MyBatis mapper was found in '[com.bluecone.app.member.infra.persistence.mapper]' package.
```

### ✅ 已修复

已移除以下文件中的重复 `@MapperScan` 注解：
- `app-member/src/main/java/com/bluecone/app/member/config/MemberAutoConfiguration.java`
- `app-campaign/src/main/java/com/bluecone/app/campaign/infrastructure/config/CampaignModuleConfiguration.java`

全局 Mapper 扫描由 `app-infra/src/main/java/com/bluecone/app/infra/config/MybatisPlusConfig.java` 统一管理。

重新构建并部署后，警告将消失。

---

## 📚 详细文档

- **健康检查配置**: `docs/WECHAT_CLOUD_HEALTH_CHECK.md`
- **Docker 部署指南**: `docs/DOCKER_GUIDE.md`
- **可观测性配置**: `docs/observability-config-examples.yml`

---

## 🆘 仍然有问题？

### 检查清单

- [ ] 应用启动时间是否超过 60 秒？（查看日志中的 "Started Application in X seconds"）
- [ ] 数据库连接是否正常？（检查环境变量 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`）
- [ ] Redis 连接是否正常？（检查环境变量 `REDIS_HOST`, `REDIS_PORT`）
- [ ] 内存是否足够？（建议至少 2GB，推荐 4GB）
- [ ] CPU 是否足够？（建议至少 1 核，推荐 2 核）

### 查看详细日志

在微信云托管控制台 → 日志 → 实时日志，查看完整的启动日志。

### 手动测试健康检查

```bash
# 在本地测试
docker run -p 80:80 bluecone-app:latest

# 等待 30 秒后测试
curl http://localhost/internal/actuator/health/liveness
curl http://localhost/internal/actuator/health/readiness
```

预期响应：
```json
{"status":"UP"}
```

