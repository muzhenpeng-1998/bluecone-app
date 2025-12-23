#!/bin/bash

# Docker 构建脚本 - 适用于微信云托管环境
# 使用方法: ./docker-build.sh [环境名称]
# 环境名称: local(默认), dev, test, prod

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认配置
PROFILE="${1:-local}"
IMAGE_NAME="bluecone-app"
IMAGE_TAG="${PROFILE}-$(date +%Y%m%d-%H%M%S)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}BlueCone App Docker 构建${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Profile: ${YELLOW}${PROFILE}${NC}"
echo -e "Image: ${YELLOW}${IMAGE_NAME}:${IMAGE_TAG}${NC}"
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 构建镜像
echo -e "${GREEN}开始构建 Docker 镜像...${NC}"
docker build \
    --build-arg SPRING_PROFILE=${PROFILE} \
    -t ${IMAGE_NAME}:${IMAGE_TAG} \
    -t ${IMAGE_NAME}:${PROFILE}-latest \
    -t ${IMAGE_NAME}:latest \
    .

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}构建成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "镜像标签:"
    echo -e "  - ${YELLOW}${IMAGE_NAME}:${IMAGE_TAG}${NC}"
    echo -e "  - ${YELLOW}${IMAGE_NAME}:${PROFILE}-latest${NC}"
    echo -e "  - ${YELLOW}${IMAGE_NAME}:latest${NC}"
    echo ""
    echo -e "${GREEN}运行容器:${NC}"
    echo -e "  docker run -p 8080:80 -e SPRING_PROFILES_ACTIVE=${PROFILE} ${IMAGE_NAME}:latest"
    echo ""
    echo -e "${GREEN}查看镜像大小:${NC}"
    docker images ${IMAGE_NAME}:${IMAGE_TAG}
else
    echo -e "${RED}构建失败！${NC}"
    exit 1
fi

