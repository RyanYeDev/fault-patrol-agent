#!/bin/bash
# 镜像推送脚本：推送到你的镜像仓库
# 用法：REGISTRY=your-registry.example.com bash fault-patrol-agent-app/push.sh [镜像标签]
set -e

TAG="${1:-latest}"
REGISTRY="${REGISTRY:-registry.example.com}"
NAMESPACE="${NAMESPACE:-your-namespace}"
IMAGE_NAME="fault-patrol-agent-app"

echo "Tagging image..."
docker tag fault-patrol-agent-app:${TAG} ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${TAG}

echo "Pushing image..."
docker push ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${TAG}

echo "Done: ${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${TAG}"
