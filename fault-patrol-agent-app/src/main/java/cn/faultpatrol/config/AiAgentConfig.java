package cn.faultpatrol.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiAgentConfig {

    /**
     * -- 删除旧的表（如果存在）
     * DROP TABLE IF EXISTS public.vector_store_openai;
     * <p>
     * -- 创建新的表，使用UUID作为主键
     * CREATE TABLE public.vector_store_openai (
     * id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     * content TEXT NOT NULL,
     * metadata JSONB,
     * embedding VECTOR(1024)
     * );
     * <p>
     * SELECT * FROM vector_store_openai
     */
    @Bean("vectorStore")
    public PgVectorStore pgVectorStore(OpenAiEmbeddingModel embeddingModel,
                                       @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store_rag")
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

}
