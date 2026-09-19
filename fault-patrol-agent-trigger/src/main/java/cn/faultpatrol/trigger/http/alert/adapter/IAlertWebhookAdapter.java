package cn.faultpatrol.trigger.http.alert.adapter;

import cn.faultpatrol.api.dto.AlertRequestDTO;

import java.util.Map;

/**
 * 监控告警 Webhook 适配器标准接口
 * <p>
 * 将 Prometheus Alertmanager、Grafana、微服务 Actuator、CloudEvents 等不同来源的
 * 异构告警 Payload 统一标准化为 {@link AlertRequestDTO}。
 */
public interface IAlertWebhookAdapter {

    /**
     * 判断当前适配器是否匹配给定的路径类型或消息特征
     *
     * @param sourceHint 路径或来源提示（如 alertmanager、grafana、microservice、cloudevents）
     * @param rawBody    原始 Webhook 请求体
     * @return true 如果支持处理
     */
    boolean supports(String sourceHint, String rawBody);

    /**
     * 将原始告警体转换为标准 AlertRequestDTO
     *
     * @param rawBody 原始 JSON 字符串
     * @param headers HTTP 请求头映射
     * @return 标准化告警对象
     */
    AlertRequestDTO adapt(String rawBody, Map<String, String> headers);

    /**
     * 适配器名称标识
     */
    String getSourceType();

}
