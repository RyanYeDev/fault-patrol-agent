package cn.faultpatrol.config;

import cn.faultpatrol.domain.agent.config.LlmProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 基础组件配置
 * <p>
 * - PgVectorStore：故障手册向量库（表 vector_store_rag，维度与嵌入模型一致）
 * - OpenAiEmbeddingModel：自定义构建以携带连接/读取超时（默认超时不可控）
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class AiAgentConfig {

    @Bean("vectorStore")
    public PgVectorStore pgVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
                                       @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store_rag")
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * 自定义嵌入模型 Bean：携带连接/读取超时，防止上游 API 黑洞拖垮诊断链路
     */
    @Bean("openAiEmbeddingModel")
    @Primary
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            LlmProperties llmProperties,
            @Value("${spring.ai.openai.embedding.base-url}") String baseUrl,
            @Value("${spring.ai.openai.embedding.api-key}") String apiKey,
            @Value("${spring.ai.openai.embedding.options.model:BAAI/bge-m3}") String modelName) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(llmProperties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(llmProperties.getReadTimeoutMs()));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelName)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

}
