package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 告警适配器路由工厂
 */
@Slf4j
@Component
public class AlertAdapterFactory {

    @Resource
    private List<IAlertWebhookAdapter> adapters;

    @Resource
    private StandardAlertAdapter standardAlertAdapter;

    /**
     * 根据来源提示（如 URL 路径后缀）及请求体内容匹配最佳适配器
     */
    public AlertRequestDTO adapt(String sourceHint, String rawBody, Map<String, String> headers) {
        if (adapters != null) {
            for (IAlertWebhookAdapter adapter : adapters) {
                // 跳过兜底适配器，留到最后
                if (adapter instanceof StandardAlertAdapter) {
                    continue;
                }
                if (adapter.supports(sourceHint, rawBody)) {
                    log.info("告警请求命中适配器：[{}]", adapter.getSourceType());
                    return adapter.adapt(rawBody, headers);
                }
            }
        }
        log.info("未命中专用适配器，使用标准适配器解析");
        return standardAlertAdapter.adapt(rawBody, headers);
    }

}
