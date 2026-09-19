package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微服务 Actuator / 自主上报异常 Webhook 适配器
 */
@Slf4j
@Component
public class MicroserviceActuatorAdapter implements IAlertWebhookAdapter {

    public static final String SOURCE_TYPE = "microservice";

    @Override
    public boolean supports(String sourceHint, String rawBody) {
        if (SOURCE_TYPE.equalsIgnoreCase(sourceHint)) {
            return true;
        }
        if (rawBody == null) {
            return false;
        }
        try {
            JSONObject obj = JSON.parseObject(rawBody);
            return obj.containsKey("serviceName") && (obj.containsKey("status") || obj.containsKey("reason"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AlertRequestDTO adapt(String rawBody, Map<String, String> headers) {
        JSONObject root = JSON.parseObject(rawBody);
        String serviceName = root.getString("serviceName");
        String instanceId = root.getString("instanceId");
        String status = root.getString("status");
        String reason = root.getString("reason");
        JSONObject details = root.getJSONObject("details");
        JSONObject metrics = root.getJSONObject("metrics");

        StringBuilder content = new StringBuilder();
        content.append(String.format("【微服务健康状态: %s】\n", StringUtils.defaultString(status, "DEGRADED").toUpperCase()));
        content.append(String.format("服务名称: %s\n", serviceName));
        if (StringUtils.isNotBlank(instanceId)) {
            content.append(String.format("异常实例: %s\n", instanceId));
        }
        if (StringUtils.isNotBlank(reason)) {
            content.append(String.format("异常原因: %s\n", reason));
        }
        if (details != null && !details.isEmpty()) {
            content.append("组件健康明细:\n");
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                content.append(String.format("  - %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        if (metrics != null && !metrics.isEmpty()) {
            content.append("当前关键指标快照:\n");
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                content.append(String.format("  - %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }

        String severity = "critical";
        if ("UP".equalsIgnoreCase(status) || "HEALTHY".equalsIgnoreCase(status)) {
            severity = "info";
        } else if ("DEGRADED".equalsIgnoreCase(status) || "WARN".equalsIgnoreCase(status)) {
            severity = "warning";
        }

        return AlertRequestDTO.builder()
                .alertName(String.format("[%s] 微服务健康检查异常(%s)", serviceName, StringUtils.defaultString(status, "DOWN")))
                .severity(severity)
                .source("microservice-actuator")
                .alertContent(content.toString().trim())
                .build();
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

}
