# 兼容 amd、arm 构建镜像
 docker buildx build --load --platform linux/amd64,linux/arm64 -t /fault-patrol-agent-app:1.0 -f ./Dockerfile .