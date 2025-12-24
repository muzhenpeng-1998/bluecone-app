# 微信云开发 Docker 构建 .mvn 目录问题修复

## 问题描述

在微信云开发平台构建 Docker 镜像时，出现以下错误：

```
ERROR: failed to solve: failed to compute cache key: failed to calculate checksum of ref 246ae6c4-3a93-4189-9734-547062c0edc2::49kg8teaecafc9ndrd9bbdbux: "/.mvn": not found
```

## 根本原因

`.mvn` 是一个隐藏目录（以点开头），在上传到微信云开发平台时，可能被默认排除在 Docker 构建上下文之外。虽然本地文件系统中存在该目录，但在云端构建时无法访问。

## 解决方案

### 已实施的修复

在 `.dockerignore` 文件开头添加了显式包含规则：

```dockerignore
# IMPORTANT: Include .mvn directory for Maven settings
!../.mvn/
!.mvn/**
```

这确保了 `.mvn` 目录及其所有内容都会被包含在 Docker 构建上下文中。

### .mvn 目录的作用

该目录包含 Maven 构建配置：

- `settings.xml` - Maven 镜像配置（阿里云、腾讯云镜像加速）
- `maven.config` - Maven 命令行参数配置

这些配置对于在微信云托管环境中加速依赖下载至关重要。

## 验证步骤

修复后，请按以下步骤验证：

1. **检查 .mvn 目录存在**
   ```bash
   ls -la .mvn/
   # 应该看到 settings.xml 和 maven.config
   ```

2. **检查 .dockerignore 配置**
   ```bash
   head -5 .dockerignore
   # 应该看到 !.mvn/ 的包含规则
   ```

3. **重新提交到微信云开发**
   - 提交代码到代码仓库
   - 触发微信云托管的自动构建
   - 观察构建日志，确认 `.mvn` 目录成功复制

## 预期结果

修复后，Docker 构建应该能够：

1. ✅ 成功复制 `.mvn/` 目录
2. ✅ 使用国内镜像加速 Maven 依赖下载
3. ✅ 完成完整的构建流程

## 相关文件

- `.dockerignore` - Docker 构建上下文排除规则
- `Dockerfile` - Docker 镜像构建脚本（第 7 行复制 .mvn 目录）
- `.mvn/settings.xml` - Maven 镜像配置
- `.mvn/maven.config` - Maven 参数配置

## 技术说明

### .dockerignore 的工作原理

`.dockerignore` 文件使用类似 `.gitignore` 的语法：
- 以 `#` 开头的行是注释
- 普通模式表示排除
- 以 `!` 开头的模式表示包含（覆盖之前的排除规则）

### 为什么需要显式包含

虽然 `.dockerignore` 中没有显式排除 `.mvn`，但某些 Docker 客户端或云平台可能会：
- 默认排除隐藏文件/目录（以 `.` 开头）
- 在上传构建上下文时过滤掉某些文件

通过显式添加 `!.mvn/` 规则，我们确保该目录一定会被包含。

## 故障排查

如果问题仍然存在，请检查：

1. **代码是否已提交**
   ```bash
   git status
   git add .dockerignore
   git commit -m "fix: include .mvn directory in Docker build context"
   git push
   ```

2. **微信云开发是否使用最新代码**
   - 确认触发了新的构建
   - 检查构建使用的 commit hash

3. **备用方案：内联 Maven 配置**
   如果仍然无法包含 `.mvn` 目录，可以考虑在 Dockerfile 中直接创建 settings.xml：
   ```dockerfile
   RUN mkdir -p .mvn && cat > .mvn/settings.xml <<'EOF'
   <?xml version="1.0" encoding="UTF-8"?>
   <settings>
     <!-- Maven settings content -->
   </settings>
   EOF
   ```

## 更新日期

2025-12-23

## 相关文档

- [DOCKER_GUIDE.md](./DOCKER_GUIDE.md) - Docker 构建完整指南
- [DOCKER_QUICK_REFERENCE.md](./DOCKER_QUICK_REFERENCE.md) - Docker 快速参考

