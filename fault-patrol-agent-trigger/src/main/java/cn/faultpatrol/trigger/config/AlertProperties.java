package cn.faultpatrol.trigger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 告警接入配置属性
 */
@Data
@ConfigurationProperties(prefix = "faultpatrol.alert")
public class AlertProperties {

    /**
     * 告警接入默认巡检智能体ID
     */
    private String defaultAgentId = "10001";

    /**
     * webhook 签名密钥（HMAC-SHA256），配置后强制校验 X-Webhook-Signature 请求头
     */
    private String webhookSecret = "";

    /**
     * 告警去重窗口（分钟）：窗口期内相同告警指纹合并、跳过重复诊断
     */
    private int dedupWindowMinutes = 30;

}
