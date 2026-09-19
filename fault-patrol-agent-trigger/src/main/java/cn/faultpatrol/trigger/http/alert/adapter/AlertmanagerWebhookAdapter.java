package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Prometheus Alertmanager Webhook 适配器
 */
@Slf4j
@Component
public class AlertmanagerWebhookAdapter implements IAlertWebhookAdapter {

    public static final String SOURCE_TYPE = "alertmanager";

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
            return obj.containsKey("receiver") && (obj.containsKey("alerts") || obj.containsKey("groupKey"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AlertRequestDTO adapt(String rawBody, Map<String, String> headers) {
        JSONObject root = JSON.parseObject(rawBody);
        String globalStatus = root.getString("status");
        JSONArray alerts = root.getJSONArray("alerts");

        JSONObject firstAlert = (alerts != null && !alerts.isEmpty()) ? alerts.getJSONObject(0) : null;
        JSONObject commonLabels = root.getJSONObject("commonLabels");
        JSONObject commonAnnotations = root.getJSONObject("commonAnnotations");

        String alertName = extractValue(commonLabels, firstAlert, "labels", "alertname", "未命名Prometheus告警");
        String severity = extractValue(commonLabels, firstAlert, "labels", "severity", "warning");
        String service = extractValue(commonLabels, firstAlert, "labels", "service",
                extractValue(commonLabels, firstAlert, "labels", "job", "unknown-service"));

        String summary = extractValue(commonAnnotations, firstAlert, "annotations", "summary", "");
        String description = extractValue(commonAnnotations, firstAlert, "annotations", "description", "");
        String generatorURL = firstAlert != null ? firstAlert.getString("generatorURL") : null;

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(String.format("【Prometheus Alertmanager 告警状态: %s】\n", globalStatus != null ? globalStatus.toUpperCase() : "FIRING"));
        contentBuilder.append(String.format("受影响服务: %s\n", service));
        if (StringUtils.isNotBlank(summary)) {
            contentBuilder.append(String.format("告警摘要: %s\n", summary));
        }
        if (StringUtils.isNotBlank(description)) {
            contentBuilder.append(String.format("详细描述: %s\n", description));
        }
        if (alerts != null && alerts.size() > 1) {
            contentBuilder.append(String.format("合并告警实例数: %d 个\n", alerts.size()));
        }
        if (StringUtils.isNotBlank(generatorURL)) {
            contentBuilder.append(String.format("指标表达式追踪: %s\n", generatorURL));
        }

        return AlertRequestDTO.builder()
                .alertName(String.format("[%s] %s", service, alertName))
                .severity(normalizeSeverity(severity))
                .source("prometheus-alertmanager")
                .alertContent(contentBuilder.toString().trim())
                .build();
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    private String extractValue(JSONObject common, JSONObject firstAlert, String subObjKey, String fieldKey, String defaultValue) {
        if (common != null && common.containsKey(fieldKey)) {
            return common.getString(fieldKey);
        }
        if (firstAlert != null) {
            JSONObject subObj = firstAlert.getJSONObject(subObjKey);
            if (subObj != null && subObj.containsKey(fieldKey)) {
                return subObj.getString(fieldKey);
            }
        }
        return defaultValue;
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "warning";
        String s = severity.toLowerCase();
        if (s.contains("crit") || s.contains("fatal") || s.contains("err") || s.contains("p0") || s.contains("p1")) {
            return "critical";
        }
        if (s.contains("warn") || s.contains("p2") || s.contains("p3")) {
            return "warning";
        }
        return "info";
    }

}
