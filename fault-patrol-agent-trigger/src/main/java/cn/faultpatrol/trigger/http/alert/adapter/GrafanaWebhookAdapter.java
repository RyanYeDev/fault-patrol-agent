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
 * Grafana Alerting Webhook 适配器
 */
@Slf4j
@Component
public class GrafanaWebhookAdapter implements IAlertWebhookAdapter {

    public static final String SOURCE_TYPE = "grafana";

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
            return obj.containsKey("ruleName") || (obj.containsKey("state") && obj.containsKey("evalMatches"))
                    || (obj.containsKey("title") && obj.containsKey("ruleUrl"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AlertRequestDTO adapt(String rawBody, Map<String, String> headers) {
        JSONObject root = JSON.parseObject(rawBody);
        String title = root.getString("title");
        String state = root.getString("state");
        String message = root.getString("message");
        String ruleName = root.getString("ruleName");
        String ruleUrl = root.getString("ruleUrl");

        JSONArray alerts = root.getJSONArray("alerts");
        JSONObject firstAlert = (alerts != null && !alerts.isEmpty()) ? alerts.getJSONObject(0) : null;

        String severity = "warning";
        String service = "unknown-service";
        String valueString = null;

        if (firstAlert != null) {
            JSONObject labels = firstAlert.getJSONObject("labels");
            if (labels != null) {
                if (labels.containsKey("severity")) {
                    severity = labels.getString("severity");
                }
                if (labels.containsKey("service")) {
                    service = labels.getString("service");
                } else if (labels.containsKey("job")) {
                    service = labels.getString("job");
                }
            }
            valueString = firstAlert.getString("valueString");
        }

        StringBuilder content = new StringBuilder();
        content.append(String.format("【Grafana 告警状态: %s】\n", StringUtils.defaultString(state, "alerting").toUpperCase()));
        content.append(String.format("目标服务: %s\n", service));
        if (StringUtils.isNotBlank(message)) {
            content.append(String.format("告警通知: %s\n", message));
        }
        if (StringUtils.isNotBlank(valueString)) {
            content.append(String.format("指标评估采样: %s\n", valueString));
        }
        if (StringUtils.isNotBlank(ruleUrl)) {
            content.append(String.format("Grafana 仪表盘/规则链接: %s\n", ruleUrl));
        }

        String finalName = StringUtils.isNotBlank(ruleName) ? ruleName :
                (StringUtils.isNotBlank(title) ? title : "Grafana监控告警");

        return AlertRequestDTO.builder()
                .alertName(String.format("[%s] %s", service, finalName))
                .severity(normalizeSeverity(severity))
                .source("grafana")
                .alertContent(content.toString().trim())
                .build();
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    private String normalizeSeverity(String severity) {
        if (severity == null) return "warning";
        String s = severity.toLowerCase();
        if (s.contains("crit") || s.contains("fatal") || s.contains("err") || s.contains("alerting")) {
            return "critical";
        }
        if (s.contains("warn")) {
            return "warning";
        }
        return "info";
    }

}
