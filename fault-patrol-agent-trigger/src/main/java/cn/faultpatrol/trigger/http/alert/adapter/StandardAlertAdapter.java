package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 平台原生标准告警请求适配器
 */
@Slf4j
@Component
public class StandardAlertAdapter implements IAlertWebhookAdapter {

    public static final String SOURCE_TYPE = "standard";

    @Override
    public boolean supports(String sourceHint, String rawBody) {
        // 作为保底兜底适配器
        return true;
    }

    @Override
    public AlertRequestDTO adapt(String rawBody, Map<String, String> headers) {
        try {
            return JSON.parseObject(rawBody, AlertRequestDTO.class);
        } catch (Exception e) {
            log.warn("无法按标准 AlertRequestDTO 解析，生成兜底告警对象：{}", e.getMessage());
            return AlertRequestDTO.builder()
                    .alertName("外部未知格式告警")
                    .severity("warning")
                    .source("custom-webhook")
                    .alertContent(rawBody)
                    .build();
        }
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

}
