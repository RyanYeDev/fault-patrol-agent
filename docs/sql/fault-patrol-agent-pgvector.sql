-- ********************************************************************
-- fault-patrol-agent pgvector 初始化 SQL
-- 数据库：fault_patrol_rag（docker 环境由 POSTGRES_DB 自动创建）
-- 执行方式：
--   本地：psql -U postgres -d fault_patrol_rag -f docs/sql/fault-patrol-agent-pgvector.sql
--   docker：由 pgvector 容器首次启动时自动执行（挂载到 /docker-entrypoint-initdb.d/）
-- 说明：向量维度需与嵌入模型一致，默认 1024（bge-m3）
-- ********************************************************************

-- 创建向量扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 故障手册知识库表（与 spring-ai PgVectorStore 表结构一致）
DROP TABLE IF EXISTS vector_store_rag;
CREATE TABLE vector_store_rag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1024)
);

-- 检索索引：HNSW 提升相似度检索性能
CREATE INDEX IF NOT EXISTS idx_vector_store_rag_embedding
    ON vector_store_rag USING hnsw (embedding vector_cosine_ops);

-- 按知识标签检索示例：
-- SELECT content, metadata FROM vector_store_rag
-- WHERE metadata->>'knowledge' = 'fault-handbook'
-- ORDER BY embedding <=> '...'::vector LIMIT 5;
