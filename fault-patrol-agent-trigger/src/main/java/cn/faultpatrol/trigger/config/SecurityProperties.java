package cn.faultpatrol.trigger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口安全配置属性
 * <p>
 * 分级鉴权：管理接口（/api/v1/admin/**）使用 admin-api-key，
 * 巡检接口（/api/v1/inspect/**）使用 inspect-api-key。
 * key 未配置时对应接口不鉴权（便于本地开发）。
 */
@Data
@ConfigurationProperties(prefix = "faultpatrol.security")
public class SecurityProperties {

    /**
     * 是否启用接口鉴权
     */
    private boolean enabled = true;

    /**
     * 巡检接口 API Key（诊断/告警/报告查询），请求头 X-Api-Key
     */
    private String inspectApiKey = "";

    /**
     * 管理接口 API Key（配置管理），请求头 X-Api-Key
     */
    private String adminApiKey = "";

}
