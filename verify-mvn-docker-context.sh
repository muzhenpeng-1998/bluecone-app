#!/bin/bash
# 验证 .mvn 目录是否会被包含在 Docker 构建上下文中

set -e

echo "==================================="
echo "验证 .mvn 目录 Docker 构建上下文"
echo "==================================="
echo ""

# 检查 .mvn 目录是否存在
echo "1. 检查 .mvn 目录..."
if [ -d ".mvn" ]; then
    echo "   ✅ .mvn 目录存在"
    ls -la .mvn/
else
    echo "   ❌ .mvn 目录不存在"
    exit 1
fi

echo ""

# 检查 .dockerignore 配置
echo "2. 检查 .dockerignore 配置..."
if grep -q "!.mvn/" .dockerignore; then
    echo "   ✅ .dockerignore 包含 .mvn 目录"
    echo "   配置内容："
    grep -A 1 "!.mvn/" .dockerignore | sed 's/^/      /'
else
    echo "   ⚠️  .dockerignore 未显式包含 .mvn 目录"
    echo "   建议在 .dockerignore 开头添加："
    echo "      !.mvn/"
    echo "      !.mvn/**"
fi

echo ""

# 检查 Docker 是否可用
echo "3. 检查 Docker 环境..."
if command -v docker &> /dev/null; then
    echo "   ✅ Docker 已安装"
    docker --version
    
    echo ""
    echo "4. 测试 Docker 构建上下文..."
    
    # 创建临时 Dockerfile 测试
    cat > /tmp/test-mvn-dockerfile <<'EOF'
FROM alpine:3.19
WORKDIR /test
COPY .mvn/ .mvn/
RUN echo "=== .mvn 目录内容 ===" && \
    ls -la .mvn/ && \
    echo "" && \
    echo "=== settings.xml 前 10 行 ===" && \
    head -10 .mvn/settings.xml && \
    echo "" && \
    echo "✅ SUCCESS: .mvn 目录成功复制到 Docker 镜像中"
EOF
    
    echo "   正在构建测试镜像..."
    if docker build --no-cache -t test-mvn-context -f /tmp/test-mvn-dockerfile . 2>&1 | tee /tmp/docker-build.log; then
        echo ""
        echo "   ✅ Docker 构建测试成功！"
        echo "   .mvn 目录已成功包含在构建上下文中"
    else
        echo ""
        echo "   ❌ Docker 构建测试失败"
        echo "   请查看上面的错误信息"
        exit 1
    fi
    
    # 清理
    rm -f /tmp/test-mvn-dockerfile
    docker rmi test-mvn-context 2>/dev/null || true
else
    echo "   ⚠️  Docker 未安装或不可用"
    echo "   跳过 Docker 构建测试"
    echo "   在微信云开发平台上构建时将自动验证"
fi

echo ""
echo "==================================="
echo "验证完成"
echo "==================================="
echo ""
echo "下一步："
echo "1. 提交代码更改："
echo "   git add .dockerignore"
echo "   git commit -m 'fix: include .mvn directory in Docker build context'"
echo "   git push"
echo ""
echo "2. 在微信云开发平台触发新的构建"
echo ""
echo "3. 查看构建日志，确认 .mvn 目录成功复制"
echo ""

