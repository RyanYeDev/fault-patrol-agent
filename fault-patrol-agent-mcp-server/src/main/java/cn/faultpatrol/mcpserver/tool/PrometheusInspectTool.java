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
 * Prometheus 只读巡检工具
 * <p>
 * 提供瞬时/区间指标查询、告警与采集目标状态查询，
 * 用于故障取证中的指标维度分析（告警 → 指标）。
 */
@Slf4j
@Component
public class PrometheusInspectTool {

    private final InspectToolsProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PrometheusInspectTool(InspectToolsProperties properties,
                                 @Qualifier("prometheusRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * 瞬时查询
     */
    @Tool(description = "Prometheus 瞬时指标查询（只读）。query 为 PromQL 表达式，如 'rate(http_server_requests_seconds_count[5m])'，返回当前时刻查询结果")
    public String promInstantQuery(@ToolParam(description = "PromQL 表达式") String query,
                                   @ToolParam(description = "可选时间戳（秒），缺省当前时刻") String time) {
        if (!properties.getPrometheus().isEnabled()) {
            return toJson(Map.of("error", "Prometheus 巡检工具未启用"));
        }
        try {
            StringBuilder uri = new StringBuilder("/api/v1/query?query=").append(urlEncode(query));
            if (time != null && !time.isBlank()) {
                uri.append("&time=").append(time);
            }
            String body = restClient.get().uri(uri.toString()).retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Prometheus 瞬时查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Prometheus 瞬时查询失败: " + e.getMessage()));
        }
    }

    /**
     * 区间查询
     */
    @Tool(description = "Prometheus 区间指标查询（只读），用于查看指标历史趋势。start/end 为秒级时间戳，step 如 '60s'")
    public String promRangeQuery(@ToolParam(description = "PromQL 表达式") String query,
                                 @ToolParam(description = "开始时间戳（秒）") String start,
                                 @ToolParam(description = "结束时间戳（秒）") String end,
                                 @ToolParam(description = "采样步长，如 60s") String step) {
        if (!properties.getPrometheus().isEnabled()) {
            return toJson(Map.of("error", "Prometheus 巡检工具未启用"));
        }
        try {
            String uri = "/api/v1/query_range?query=" + urlEncode(query)
                    + "&start=" + start + "&end=" + end + "&step=" + step;
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Prometheus 区间查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Prometheus 区间查询失败: " + e.getMessage()));
        }
    }

    /**
     * 活跃告警
     */
    @Tool(description = "查询 Prometheus 当前活跃告警（只读），用于核对告警维度")
    public String promListAlerts() {
        if (!properties.getPrometheus().isEnabled()) {
            return toJson(Map.of("error", "Prometheus 巡检工具未启用"));
        }
        try {
            String body = restClient.get().uri("/api/v1/alerts").retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Prometheus 告警查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Prometheus 告警查询失败: " + e.getMessage()));
        }
    }

    /**
     * 采集目标状态
     */
    @Tool(description = "查询 Prometheus 采集目标状态（只读）：各 exporter 的健康状态，用于排查指标缺失")
    public String promTargets() {
        if (!properties.getPrometheus().isEnabled()) {
            return toJson(Map.of("error", "Prometheus 巡检工具未启用"));
        }
        try {
            String body = restClient.get().uri("/api/v1/targets").retrieve().body(String.class);
            return toJson(objectMapper.readTree(body));
        } catch (Exception e) {
            log.warn("Prometheus 采集目标查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Prometheus 采集目标查询失败: " + e.getMessage()));
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
