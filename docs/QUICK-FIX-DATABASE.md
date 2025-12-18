# 数据库连接问题快速修复指南

## 问题症状

应用启动时出现以下错误：
```
Communications link failure
Connection refused
```

## 原因分析

1. **MySQL 服务未运行** - 最常见原因
2. **数据库连接配置错误** - `application-local.yml` 中的密码/地址不正确
3. **数据库不存在** - `bluecone` 数据库尚未创建

## 快速解决方案

### 方案 1：使用 Docker 快速启动 MySQL（推荐）

如果您已安装 Docker：

```bash
# 启动 MySQL 容器
docker run -d --name mysql-bluecone \
  -e MYSQL_ROOT_PASSWORD=bluecone \
  -e MYSQL_DATABASE=bluecone \
  -p 3306:3306 \
  mysql:8.3.0

# 等待几秒让 MySQL 完全启动
sleep 5

# 验证连接
docker exec -it mysql-bluecone mysql -uroot -pbluecone -e "SELECT 1"
```

然后修改 `application-local.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bluecone?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: bluecone  # 与 Docker 容器中的密码一致
```

### 方案 2：使用本地 MySQL 服务

#### macOS (Homebrew)

```bash
# 检查 MySQL 是否安装
brew list mysql

# 启动 MySQL 服务
brew services start mysql

# 或使用 mysqld_safe 启动
mysqld_safe --user=mysql &

# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bluecone;"
```

然后修改 `application-local.yml` 中的密码为您的实际 MySQL root 密码。

#### Linux

```bash
# 启动 MySQL 服务
sudo systemctl start mysql
# 或
sudo service mysql start

# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bluecone;"
```

### 方案 3：临时禁用 Flyway（仅用于快速验证）

如果暂时无法启动数据库，可以临时禁用 Flyway 来验证其他功能：

在 `application-local.yml` 中添加：
```yaml
spring:
  flyway:
    enabled: false
```

**注意**：这会导致数据库迁移不会执行，仅用于快速验证应用启动，不推荐用于开发。

## 验证步骤

### 1. 检查 MySQL 服务状态

```bash
# macOS
brew services list | grep mysql

# Linux
sudo systemctl status mysql

# Docker
docker ps | grep mysql
```

### 2. 测试数据库连接

```bash
# 使用命令行测试
mysql -h localhost -P 3306 -u root -p -e "SELECT 1"

# 或使用 Docker
docker exec -it mysql-bluecone mysql -uroot -pbluecone -e "SELECT 1"
```

### 3. 检查 application-local.yml 配置

确保以下配置正确：
- `url`: 数据库地址和端口
- `username`: 数据库用户名
- `password`: 数据库密码（**不是模板值 `your_local_password`**）

### 4. 创建数据库（如果不存在）

```sql
CREATE DATABASE IF NOT EXISTS bluecone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 常见问题

### Q: 如何找到我的 MySQL root 密码？

**macOS (Homebrew)**:
- 默认可能为空密码
- 或查看安装时的提示信息
- 或重置密码：`mysqladmin -u root password 'newpassword'`

**Linux**:
- 查看 `/etc/mysql/debian.cnf`（Debian/Ubuntu）
- 或使用 `sudo mysql` 无密码登录后重置

### Q: 端口 3306 被占用怎么办？

```bash
# 检查端口占用
lsof -i :3306

# 修改 application-local.yml 使用其他端口
url: jdbc:mysql://localhost:3307/bluecone?...
```

### Q: 忘记密码怎么办？

```bash
# macOS/Linux - 重置 MySQL root 密码
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'newpassword';
FLUSH PRIVILEGES;
```

## 启动应用

配置完成后，使用 local profile 启动：

```bash
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-application -am spring-boot:run -Dspring-boot.run.profiles=local
```

---

**提示**：如果仍然无法连接，请检查：
1. 防火墙是否阻止了 3306 端口
2. MySQL 是否监听在 `localhost` 而不是 `127.0.0.1`
3. 用户权限是否正确（root 用户是否有远程连接权限）

