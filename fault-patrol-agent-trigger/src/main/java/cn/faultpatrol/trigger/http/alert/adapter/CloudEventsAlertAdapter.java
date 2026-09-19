package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CNCF CloudEvents 1.0 规范告警适配器
 */
@Slf4j
@Component
public class CloudEventsAlertAdapter implements IAlertWebhookAdapter {

    public static final String SOURCE_TYPE = "cloudevents";

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
            return obj.containsKey("specversion") && obj.containsKey("type") && obj.containsKey("source");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AlertRequestDTO adapt(String rawBody, Map<String, String> headers) {
        JSONObject root = JSON.parseObject(rawBody);
        String eventId = root.getString("id");
        String eventType = root.getString("type");
        String eventSource = root.getString("source");
        String time = root.getString("time");
        Object dataObj = root.get("data");

        StringBuilder content = new StringBuilder();
        content.append(String.format("【CloudEvents 事件类型: %s】\n", eventType));
        content.append(String.format("事件来源: %s\n", eventSource));
        if (StringUtils.isNotBlank(time)) {
            content.append(String.format("发生时间: %s\n", time));
        }
        if (StringUtils.isNotBlank(eventId)) {
            content.append(String.format("事件ID: %s\n", eventId));
        }
        if (dataObj != null) {
            content.append(String.format("事件数据内容:\n%s\n", JSON.toJSONString(dataObj, true)));
        }

        return AlertRequestDTO.builder()
                .alertName(String.format("[%s] %s", eventSource, eventType))
                .severity("warning")
                .source("cloudevents")
                .alertContent(content.toString().trim())
                .build();
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

}
