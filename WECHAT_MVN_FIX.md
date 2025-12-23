# 微信云托管 Maven 配置修复说明

## 问题描述

在微信云托管平台构建 Docker 镜像时，遇到以下错误：

```
ERROR: failed to solve: failed to compute cache key: failed to calculate checksum of ref: "/.mvn": not found
```

## 根本原因

微信云托管平台使用的 Docker 构建环境对以点（`.`）开头的目录处理存在问题，无法正确识别和复制 `.mvn/` 目录到构建上下文中。

## 解决方案

将 `.mvn/` 目录复制为 `mvn-config/` 目录（不以点开头），然后在 Dockerfile 中从 `mvn-config/` 复制到容器内的 `.mvn/` 位置。

### 修改内容

1. **创建 mvn-config 目录**
   ```bash
   cp -r .mvn mvn-config
   ```

2. **修改 Dockerfile**
   ```dockerfile
   # 原来：
   COPY .mvn/ .mvn/
   
   # 修改为：
   COPY mvn-config/ .mvn/
   ```

3. **提交 mvn-config 到 Git**
   `mvn-config/` 目录需要提交到 Git，这样微信云托管平台才能访问它。

## 技术细节

- `.mvn/` 目录包含 Maven 的自定义配置（如国内镜像源）
- 在容器内部，Maven 仍然从 `.mvn/settings.xml` 读取配置
- 只是在 Docker COPY 阶段使用不带点的目录名
- 这个方案不影响本地开发，本地仍然使用 `.mvn/` 目录

## 验证步骤

提交代码后，在微信云托管平台触发新的构建，应该能看到：

```
[build 3/44] COPY mvn-config/ .mvn/
```

而不是之前的错误信息。

## 相关文件

- `Dockerfile` - 构建配置
- `.mvn/` - 原始 Maven 配置（本地使用）
- `mvn-config/` - Docker 构建用的 Maven 配置副本（已提交到 Git）

## 注意事项

如果修改了 `.mvn/` 目录中的配置文件，需要同步更新 `mvn-config/` 目录：

```bash
cp -r .mvn/* mvn-config/
```

或者在提交前运行：

```bash
rsync -av --delete .mvn/ mvn-config/
```

