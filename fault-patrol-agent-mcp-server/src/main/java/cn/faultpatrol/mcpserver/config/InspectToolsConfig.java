package cn.faultpatrol.mcpserver.config;

import cn.faultpatrol.mcpserver.tool.BusinessDataInspectTool;
import cn.faultpatrol.mcpserver.tool.ContainerInspectTool;
import cn.faultpatrol.mcpserver.tool.JaegerInspectTool;
import cn.faultpatrol.mcpserver.tool.PrometheusInspectTool;
import cn.faultpatrol.mcpserver.tool.RabbitMqInspectTool;
import cn.faultpatrol.mcpserver.tool.RedisInspectTool;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 巡检工具装配配置
 * <p>
 * 将所有巡检工具注册为 MCP ToolCallback，MCP 服务器启动时自动发布；
 * 各外部服务客户端按 enabled 开关与超时配置构建。
 */
@Configuration
@EnableConfigurationProperties(InspectToolsProperties.class)
public class InspectToolsConfig {

    /**
     * MCP 工具注册：统一对外发布巡检工具
     */
    @Bean
    public ToolCallbackProvider inspectToolCallbackProvider(RedisInspectTool redisInspectTool,
                                                            RabbitMqInspectTool rabbitMqInspectTool,
                                                            PrometheusInspectTool prometheusInspectTool,
                                                            JaegerInspectTool jaegerInspectTool,
                                                            ContainerInspectTool containerInspectTool,
                                                            BusinessDataInspectTool businessDataInspectTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(redisInspectTool,
                        rabbitMqInspectTool,
                        prometheusInspectTool,
                        jaegerInspectTool,
                        containerInspectTool,
                        businessDataInspectTool)
                .build();
    }

    /**
     * Redis 连接（启用时构建；Lettuce 懒连接，Redis 不可达不阻塞启动）
     */
    @Bean
    @ConditionalOnProperty(prefix = "faultpatrol.tools.redis", name = "enabled", havingValue = "true")
    public StringRedisTemplate inspectStringRedisTemplate(InspectToolsProperties properties) {
        InspectToolsProperties.Redis redis = properties.getRedis();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        if (StringUtils.isNotBlank(redis.getPassword())) {
            configuration.setPassword(redis.getPassword());
        }
        configuration.setDatabase(redis.getDatabase());

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.setTimeout(redis.getTimeoutMs());
        connectionFactory.afterPropertiesSet();
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * RabbitMQ 管理 API 客户端
     */
    @Bean
    public RestClient mqRestClient(InspectToolsProperties properties) {
        return buildRestClient(properties.getRabbitmq().getManagementUrl(), properties.getRabbitmq().getTimeout());
    }

    /**
     * Prometheus API 客户端
     */
    @Bean
    public RestClient prometheusRestClient(InspectToolsProperties properties) {
        return buildRestClient(properties.getPrometheus().getBaseUrl(), properties.getPrometheus().getTimeout());
    }

    /**
     * Jaeger API 客户端
     */
    @Bean
    public RestClient jaegerRestClient(InspectToolsProperties properties) {
        return buildRestClient(properties.getJaeger().getBaseUrl(), properties.getJaeger().getTimeout());
    }

    /**
     * 构建带连接/读取超时的 RestClient
     */
    private RestClient buildRestClient(String baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

}
