#!/bin/bash

# BlueCone Application Build Script
# 确保使用 Java 21 进行构建

# 设置 Java 21
export JAVA_HOME=/Users/zhenpengmu/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "=========================================="
echo "BlueCone 项目构建"
echo "=========================================="
echo "使用 Java: $(java -version 2>&1 | head -1)"
echo "JAVA_HOME: $JAVA_HOME"
echo "=========================================="
echo ""

# 进入项目目录
cd "$(dirname "$0")"

# 执行构建
echo "开始构建项目..."
mvn clean package -DskipTests -Dmaven.test.skip=true

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ 构建成功！"
    echo "=========================================="
    echo "JAR 文件位置: app-application/target/bluecone-app.jar"
    echo ""
    echo "运行应用："
    echo "  ./run-app.sh"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "❌ 构建失败"
    echo "=========================================="
    exit 1
fi

