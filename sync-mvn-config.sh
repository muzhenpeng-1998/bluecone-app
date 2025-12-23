#!/bin/bash
# 同步 .mvn 配置到 mvn-config 目录
# 用于微信云托管 Docker 构建

set -e

echo "同步 Maven 配置..."
echo "从: .mvn/"
echo "到: mvn-config/"
echo ""

# 确保 .mvn 目录存在
if [ ! -d ".mvn" ]; then
    echo "❌ 错误: .mvn 目录不存在"
    exit 1
fi

# 创建或更新 mvn-config 目录
rsync -av --delete .mvn/ mvn-config/

echo ""
echo "✅ 同步完成！"
echo ""
echo "mvn-config 目录内容："
ls -la mvn-config/
echo ""
echo "提示: 提交代码前请运行此脚本确保配置同步"

