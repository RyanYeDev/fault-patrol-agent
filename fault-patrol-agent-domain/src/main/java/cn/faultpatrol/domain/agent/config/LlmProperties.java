package cn.faultpatrol.domain.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM 调用超时配置
 * <p>
 * 适用于装配生成的 OpenAiApi 对话客户端与向量嵌入客户端，
 * 防止上游 API 网络黑洞导致诊断链路无限等待。
 */
@Data
@ConfigurationProperties(prefix = "faultpatrol.llm")
public class LlmProperties {

    /**
     * TCP 连接超时（毫秒）
     */
    private int connectTimeoutMs = 30000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeoutMs = 120000;

}
