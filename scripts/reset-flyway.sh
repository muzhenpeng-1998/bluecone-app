#!/bin/bash
# Flyway 历史重置脚本（仅用于开发环境）
# 
# 说明：此脚本会删除 Flyway 历史表，让 Flyway 重新执行所有迁移脚本
# 警告：仅用于开发环境，生产环境禁止使用！

set -e

# 从环境变量或配置文件读取数据库连接信息
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-bluecone}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-}

echo "=========================================="
echo "Flyway 历史重置脚本"
echo "=========================================="
echo "数据库: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "用户: ${DB_USER}"
echo ""
echo "警告：此操作会删除 Flyway 历史表！"
echo "按 Ctrl+C 取消，或按 Enter 继续..."
read

# 构建 MySQL 命令
if [ -z "$DB_PASSWORD" ]; then
    MYSQL_CMD="mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER}"
else
    MYSQL_CMD="mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} -p${DB_PASSWORD}"
fi

echo "正在删除 Flyway 历史表..."
$MYSQL_CMD ${DB_NAME} <<EOF
DROP TABLE IF EXISTS flyway_schema_history;
SELECT 'Flyway 历史表已删除' AS status;
EOF

echo ""
echo "✅ Flyway 历史表已删除"
echo ""
echo "现在可以重新启动应用，Flyway 会重新执行所有迁移脚本："
echo "  mvn -pl app-application -am spring-boot:run -Dspring-boot.run.profiles=local"

