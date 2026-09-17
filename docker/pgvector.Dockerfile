# pgvector 镜像（自建）：postgres:16 + PGDG 源安装 pgvector 扩展
# 说明：官方 pgvector/pgvector 镜像在部分网络环境下不可用，此镜像构建结果与其等价
FROM postgres:16

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-16-pgvector \
    && rm -rf /var/lib/apt/lists/*
