#!/bin/bash

# Docker 测试脚本 - 验证 Docker 构建和运行是否正常
# 使用方法: ./docker-test.sh

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
IMAGE_NAME="bluecone-app"
CONTAINER_NAME="bluecone-app-test"
TEST_PORT="8888"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}BlueCone App Docker 测试${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 清理函数
cleanup() {
    echo -e "${YELLOW}清理测试环境...${NC}"
    docker stop ${CONTAINER_NAME} 2>/dev/null || true
    docker rm ${CONTAINER_NAME} 2>/dev/null || true
    echo -e "${GREEN}清理完成${NC}"
}

# 注册清理函数
trap cleanup EXIT

# 1. 检查 Docker 是否运行
echo -e "${BLUE}[1/6] 检查 Docker 环境...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker 运行正常${NC}"
echo ""

# 2. 检查镜像是否存在
echo -e "${BLUE}[2/6] 检查镜像...${NC}"
if ! docker images ${IMAGE_NAME}:latest | grep -q ${IMAGE_NAME}; then
    echo -e "${YELLOW}警告: 镜像不存在，开始构建...${NC}"
    ./docker-build.sh local
else
    echo -e "${GREEN}✓ 镜像已存在${NC}"
fi
echo ""

# 3. 启动测试容器
echo -e "${BLUE}[3/6] 启动测试容器...${NC}"
docker run -d \
    --name ${CONTAINER_NAME} \
    -p ${TEST_PORT}:80 \
    -e SPRING_PROFILES_ACTIVE=local \
    ${IMAGE_NAME}:latest

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 容器启动成功${NC}"
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    exit 1
fi
echo ""

# 4. 等待应用启动
echo -e "${BLUE}[4/6] 等待应用启动...${NC}"
echo -e "${YELLOW}预计需要 30-60 秒...${NC}"

MAX_WAIT=120
WAIT_TIME=0
INTERVAL=5

while [ $WAIT_TIME -lt $MAX_WAIT ]; do
    if docker logs ${CONTAINER_NAME} 2>&1 | grep -q "Started.*Application"; then
        echo -e "${GREEN}✓ 应用启动成功${NC}"
        break
    fi
    
    if docker logs ${CONTAINER_NAME} 2>&1 | grep -q "Exception\|Error"; then
        echo -e "${RED}✗ 应用启动失败，查看日志:${NC}"
        docker logs ${CONTAINER_NAME}
        exit 1
    fi
    
    echo -e "${YELLOW}等待中... (${WAIT_TIME}s/${MAX_WAIT}s)${NC}"
    sleep $INTERVAL
    WAIT_TIME=$((WAIT_TIME + INTERVAL))
done

if [ $WAIT_TIME -ge $MAX_WAIT ]; then
    echo -e "${RED}✗ 应用启动超时${NC}"
    echo -e "${YELLOW}查看日志:${NC}"
    docker logs ${CONTAINER_NAME}
    exit 1
fi
echo ""

# 5. 测试健康检查
echo -e "${BLUE}[5/6] 测试健康检查...${NC}"
if command -v curl &> /dev/null; then
    HEALTH_CHECK=$(curl -s http://localhost:${TEST_PORT}/actuator/health 2>/dev/null || echo "")
    if echo "$HEALTH_CHECK" | grep -q "UP"; then
        echo -e "${GREEN}✓ 健康检查通过${NC}"
        echo -e "${YELLOW}响应: ${HEALTH_CHECK}${NC}"
    else
        echo -e "${YELLOW}⚠ 健康检查端点可能未启用或应用未完全启动${NC}"
    fi
else
    echo -e "${YELLOW}⚠ curl 未安装，跳过健康检查测试${NC}"
fi
echo ""

# 6. 验证环境变量
echo -e "${BLUE}[6/6] 验证环境变量...${NC}"
SPRING_PROFILE=$(docker exec ${CONTAINER_NAME} sh -c 'echo $SPRING_PROFILES_ACTIVE' 2>/dev/null || echo "")
if [ "$SPRING_PROFILE" = "local" ]; then
    echo -e "${GREEN}✓ Spring Profile 配置正确: ${SPRING_PROFILE}${NC}"
else
    echo -e "${RED}✗ Spring Profile 配置错误: ${SPRING_PROFILE}${NC}"
fi
echo ""

# 显示测试结果
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}测试完成！${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}容器信息:${NC}"
echo -e "  容器名称: ${CONTAINER_NAME}"
echo -e "  访问地址: http://localhost:${TEST_PORT}"
echo -e "  Spring Profile: ${SPRING_PROFILE}"
echo ""
echo -e "${GREEN}常用命令:${NC}"
echo -e "  查看日志: ${YELLOW}docker logs -f ${CONTAINER_NAME}${NC}"
echo -e "  进入容器: ${YELLOW}docker exec -it ${CONTAINER_NAME} sh${NC}"
echo -e "  停止容器: ${YELLOW}docker stop ${CONTAINER_NAME}${NC}"
echo -e "  删除容器: ${YELLOW}docker rm ${CONTAINER_NAME}${NC}"
echo ""
echo -e "${YELLOW}提示: 测试容器将在脚本退出时自动清理${NC}"
echo -e "${YELLOW}如需保留容器，请按 Ctrl+C 退出脚本${NC}"
echo ""

# 询问是否查看日志
read -p "是否查看容器日志？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}查看日志（按 Ctrl+C 退出）...${NC}"
    docker logs -f ${CONTAINER_NAME}
fi

