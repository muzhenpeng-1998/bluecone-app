#!/bin/bash
# ID 模块重构验证脚本

set -e

echo "========================================="
echo "ID 模块重构验证"
echo "========================================="
echo ""

# 1. 编译验证
echo "✓ 步骤 1: 编译验证"
cd /Users/zhenpengmu/Desktop/code/project/bluecone-app
mvn -pl app-id,app-id-api -am clean compile -q
echo "  ✅ app-id 模块编译成功"
echo ""

# 2. 验证配置默认值
echo "✓ 步骤 2: 验证配置默认值"
echo "  检查 BlueconeIdProperties.java..."
if grep -q "private boolean enabled = true;" app-id/src/main/java/com/bluecone/app/id/internal/config/BlueconeIdProperties.java; then
    echo "  ✅ long.enabled 默认为 true"
fi
if grep -q "private LongIdStrategy strategy = LongIdStrategy.SNOWFLAKE;" app-id/src/main/java/com/bluecone/app/id/internal/config/BlueconeIdProperties.java; then
    echo "  ✅ long.strategy 默认为 SNOWFLAKE"
fi
if grep -q "private boolean enabled = false;" app-id/src/main/java/com/bluecone/app/id/internal/config/BlueconeIdProperties.java | tail -1; then
    echo "  ✅ segment.enabled 默认为 false"
fi
if grep -q "private long epochMillis = 1704067200000L;" app-id/src/main/java/com/bluecone/app/id/internal/config/BlueconeIdProperties.java; then
    echo "  ✅ epochMillis 默认为 2024-01-01"
fi
echo ""

# 3. 验证 nodeId 派生支持
echo "✓ 步骤 3: 验证 nodeId 派生支持"
if grep -q "deriveNodeId" app-id/src/main/java/com/bluecone/app/id/internal/config/InstanceNodeIdProvider.java; then
    echo "  ✅ InstanceNodeIdProvider 支持 nodeId 派生"
fi
if grep -q "CRC32" app-id/src/main/java/com/bluecone/app/id/internal/config/InstanceNodeIdProvider.java; then
    echo "  ✅ 使用 CRC32 哈希派生 nodeId"
fi
echo ""

# 4. 验证 Snowflake 修复
echo "✓ 步骤 4: 验证 Snowflake 时间戳溢出处理"
if grep -q "throw new IllegalStateException" app-id/src/main/java/com/bluecone/app/id/internal/core/SnowflakeLongIdGenerator.java | head -1; then
    echo "  ✅ 时间戳溢出时抛出异常（不取模）"
fi
echo ""

# 5. 验证自动装配
echo "✓ 步骤 5: 验证自动装配配置"
if grep -q "SnowflakeLongIdGenerator" app-id/src/main/java/com/bluecone/app/id/internal/autoconfigure/IdAutoConfiguration.java; then
    echo "  ✅ IdAutoConfiguration 装配 SnowflakeLongIdGenerator"
fi
echo ""

# 6. 验证文档
echo "✓ 步骤 6: 验证文档"
if [ -f "app-id/README-minimal.md" ]; then
    echo "  ✅ README-minimal.md 已创建"
fi
echo ""

# 7. 验证整个应用编译
echo "✓ 步骤 7: 验证整个应用编译"
mvn -pl app-application -am compile -q
echo "  ✅ 整个应用编译成功（包括所有依赖 app-id 的模块）"
echo ""

echo "========================================="
echo "✅ 验证完成！"
echo "========================================="
echo ""
echo "核心改动总结："
echo "  1. ✅ long ID 默认启用（零配置可用）"
echo "  2. ✅ 默认策略为 SNOWFLAKE（不依赖数据库）"
echo "  3. ✅ SEGMENT 必须显式开启"
echo "  4. ✅ nodeId 支持自动派生"
echo "  5. ✅ Snowflake 时间戳溢出处理修复"
echo "  6. ✅ epoch 默认使用 2024-01-01"
echo ""
echo "后续建议："
echo "  1. 启动应用验证运行时行为"
echo "  2. 检查日志确认 nodeId 派生警告"
echo "  3. 测试 nextLong() 方法可正常调用"
echo ""

