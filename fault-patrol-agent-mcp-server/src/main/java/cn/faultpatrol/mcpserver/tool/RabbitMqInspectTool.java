package cn.faultpatrol.mcpserver.tool;

import cn.faultpatrol.mcpserver.config.InspectToolsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 只读巡检工具
 * <p>
 * 通过 RabbitMQ 管理 HTTP API 提供队列、连接、消费者与积压状态等只读查询，
 * 不提供任何管理操作（创建/删除/清空队列等）。
 */
@Slf4j
@Component
public class RabbitMqInspectTool {

    private final InspectToolsProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMqInspectTool(InspectToolsProperties properties,
                               @Qualifier("mqRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * 集群概览
     */
    @Tool(description = "查询 RabbitMQ 集群概览（只读）：消息速率、连接数、队列数、消息总量等")
    public String mqOverview() {
        if (!properties.getRabbitmq().isEnabled()) {
            return toJson(Map.of("error", "RabbitMQ 巡检工具未启用"));
        }
        try {
            String body = get("/api/overview");
            JsonNode node = objectMapper.readTree(body);
            Map<String, Object> result = new HashMap<>();
            result.put("clusterName", node.path("cluster_name").asText());
            result.put("queueTotals", node.path("queue_totals"));
            result.put("messageStats", node.path("message_stats"));
            result.put("connections", node.path("object_totals").path("connections").asInt());
            result.put("channels", node.path("object_totals").path("channels").asInt());
            result.put("consumers", node.path("object_totals").path("consumers").asInt());
            result.put("queues", node.path("object_totals").path("queues").asInt());
            result.put("exchanges", node.path("object_totals").path("exchanges").asInt());
            return toJson(result);
        } catch (Exception e) {
            log.warn("RabbitMQ 概览查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "RabbitMQ 概览查询失败: " + e.getMessage()));
        }
    }

    /**
     * 队列列表（含积压深度）
     */
    @Tool(description = "查询 RabbitMQ 队列列表（只读）：队列名、消息积压数、消费者数、状态，用于排查 MQ 积压")
    public String mqListQueues() {
        if (!properties.getRabbitmq().isEnabled()) {
            return toJson(Map.of("error", "RabbitMQ 巡检工具未启用"));
        }
        try {
            String body = get("/api/queues");
            JsonNode queues = objectMapper.readTree(body);
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode queue : queues) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", queue.path("name").asText());
                item.put("vhost", queue.path("vhost").asText());
                item.put("messages", queue.path("messages").asLong());
                item.put("messagesReady", queue.path("messages_ready").asLong());
                item.put("messagesUnacknowledged", queue.path("messages_unacknowledged").asLong());
                item.put("consumers", queue.path("consumers").asInt());
                item.put("state", queue.path("state").asText());
                result.add(item);
            }
            return toJson(Map.of("count", result.size(), "queues", result));
        } catch (Exception e) {
            log.warn("RabbitMQ 队列列表查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "RabbitMQ 队列列表查询失败: " + e.getMessage()));
        }
    }

    /**
     * 单个队列详情
     */
    @Tool(description = "查询 RabbitMQ 单个队列详情（只读）：积压、消费者、消息速率等")
    public String mqQueueInfo(@ToolParam(description = "队列名称") String name) {
        if (!properties.getRabbitmq().isEnabled()) {
            return toJson(Map.of("error", "RabbitMQ 巡检工具未启用"));
        }
        try {
            String body = get("/api/queues/%2F/" + encodePath(name));
            JsonNode queue = objectMapper.readTree(body);
            Map<String, Object> item = new HashMap<>();
            item.put("name", queue.path("name").asText());
            item.put("messages", queue.path("messages").asLong());
            item.put("messagesReady", queue.path("messages_ready").asLong());
            item.put("messagesUnacknowledged", queue.path("messages_unacknowledged").asLong());
            item.put("consumers", queue.path("consumers").asInt());
            item.put("state", queue.path("state").asText());
            item.put("messageStats", queue.path("message_stats"));
            return toJson(item);
        } catch (Exception e) {
            log.warn("RabbitMQ 队列详情查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "RabbitMQ 队列详情查询失败: " + e.getMessage()));
        }
    }

    /**
     * 连接列表
     */
    @Tool(description = "查询 RabbitMQ 连接列表（只读）：客户端地址、通道数、状态")
    public String mqListConnections() {
        if (!properties.getRabbitmq().isEnabled()) {
            return toJson(Map.of("error", "RabbitMQ 巡检工具未启用"));
        }
        try {
            String body = get("/api/connections");
            JsonNode connections = objectMapper.readTree(body);
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode connection : connections) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", connection.path("name").asText());
                item.put("clientHost", connection.path("client_properties").path("host").asText());
                item.put("clientPort", connection.path("client_properties").path("port").asText());
                item.put("state", connection.path("state").asText());
                item.put("channels", connection.path("channels").asInt());
                item.put("recvOct", connection.path("recv_oct").asLong());
                item.put("sendOct", connection.path("send_oct").asLong());
                result.add(item);
            }
            return toJson(Map.of("count", result.size(), "connections", result));
        } catch (Exception e) {
            log.warn("RabbitMQ 连接列表查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "RabbitMQ 连接列表查询失败: " + e.getMessage()));
        }
    }

    /**
     * 消费者列表
     */
    @Tool(description = "查询 RabbitMQ 消费者列表（只读）：消费队列、通道、ack 模式")
    public String mqListConsumers() {
        if (!properties.getRabbitmq().isEnabled()) {
            return toJson(Map.of("error", "RabbitMQ 巡检工具未启用"));
        }
        try {
            String body = get("/api/consumers");
            JsonNode consumers = objectMapper.readTree(body);
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode consumer : consumers) {
                Map<String, Object> item = new HashMap<>();
                item.put("queue", consumer.path("queue").path("name").asText());
                item.put("channelNumber", consumer.path("channel_details").path("number").asInt());
                item.put("prefetchCount", consumer.path("prefetch_count").asInt());
                item.put("ackRequired", consumer.path("ack_required").asBoolean());
                result.add(item);
            }
            return toJson(Map.of("count", result.size(), "consumers", result));
        } catch (Exception e) {
            log.warn("RabbitMQ 消费者列表查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "RabbitMQ 消费者列表查询失败: " + e.getMessage()));
        }
    }

    private String get(String path) {
        String username = properties.getRabbitmq().getUsername();
        String password = properties.getRabbitmq().getPassword();
        return restClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION,
                        "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    private String encodePath(String name) {
        return name.replace(" ", "%20");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }

}
