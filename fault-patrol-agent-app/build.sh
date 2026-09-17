#!/bin/bash
# 主应用镜像构建脚本（在仓库根目录执行）
# 用法：bash fault-patrol-agent-app/build.sh [镜像标签]

TAG="${1:-latest}"

# 构建主应用镜像（多阶段构建，自动执行 Maven 打包）
docker build -t fault-patrol-agent-app:${TAG} -f ./Dockerfile ..

# 多架构镜像示例：
# docker buildx build --platform linux/amd64,linux/arm64 \
#   -t fault-patrol-agent-app:${TAG} -f ./Dockerfile .. --push
