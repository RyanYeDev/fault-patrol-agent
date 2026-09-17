package cn.faultpatrol.mcpserver.tool;

import cn.faultpatrol.mcpserver.config.InspectToolsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Jaeger 只读巡检工具
 * <p>
 * 提供服务列表、操作列表、链路检索与单链路详情查询，
 * 用于故障取证中的链路维度分析（指标 → 链路）。
 */
@Slf4j
@Component
public class JaegerInspectTool {

    private final InspectToolsProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JaegerInspectTool(InspectToolsProperties properties,
                             @Qualifier("jaegerRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * 服务列表
     */
    @Tool(description = "查询 Jaeger 服务列表（只读）")
    public String jaegerListServices() {
        if (!properties.getJaeger().isEnabled()) {
            return toJson(Map.of("error", "Jaeger 巡检工具未启用"));
        }
        try {
            String body = restClient.get().uri("/api/services").retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Jaeger 服务列表查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Jaeger 服务列表查询失败: " + e.getMessage()));
        }
    }

    /**
     * 服务操作列表
     */
    @Tool(description = "查询 Jaeger 指定服务的操作列表（只读）")
    public String jaegerListOperations(@ToolParam(description = "服务名") String service) {
        if (!properties.getJaeger().isEnabled()) {
            return toJson(Map.of("error", "Jaeger 巡检工具未启用"));
        }
        try {
            String body = restClient.get().uri("/api/operations?service=" + urlEncode(service)).retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Jaeger 操作列表查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Jaeger 操作列表查询失败: " + e.getMessage()));
        }
    }

    /**
     * 链路检索
     */
    @Tool(description = "按服务/操作检索 Jaeger 链路（只读）。tags 格式为 JSON，如 {\"error\":\"true\"}；返回 traceId 列表及简要信息")
    public String jaegerSearchTraces(@ToolParam(description = "服务名") String service,
                                     @ToolParam(description = "操作名，可选") String operation,
                                     @ToolParam(description = "最大返回条数，默认 20") Integer limit,
                                     @ToolParam(description = "标签过滤 JSON，可选，如 {\"error\":\"true\"}") String tags) {
        if (!properties.getJaeger().isEnabled()) {
            return toJson(Map.of("error", "Jaeger 巡检工具未启用"));
        }
        try {
            StringBuilder uri = new StringBuilder("/api/traces?service=").append(urlEncode(service));
            if (operation != null && !operation.isBlank()) {
                uri.append("&operation=").append(urlEncode(operation));
            }
            if (limit != null && limit > 0) {
                uri.append("&limit=").append(limit);
            }
            if (tags != null && !tags.isBlank()) {
                uri.append("&tags=").append(urlEncode(tags));
            }
            String body = restClient.get().uri(uri.toString()).retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Jaeger 链路检索失败: {}", e.getMessage());
            return toJson(Map.of("error", "Jaeger 链路检索失败: " + e.getMessage()));
        }
    }

    /**
     * 单链路详情
     */
    @Tool(description = "查询 Jaeger 指定 traceId 的完整链路详情（只读）：span 树、耗时、标签与日志")
    public String jaegerGetTrace(@ToolParam(description = "traceId") String traceId) {
        if (!properties.getJaeger().isEnabled()) {
            return toJson(Map.of("error", "Jaeger 巡检工具未启用"));
        }
        try {
            String body = restClient.get().uri("/api/traces/" + urlEncode(traceId)).retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Jaeger 链路详情查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Jaeger 链路详情查询失败: " + e.getMessage()));
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }

}
