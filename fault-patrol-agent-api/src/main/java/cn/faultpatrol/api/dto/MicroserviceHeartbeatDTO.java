package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 被巡检微服务主动注册/心跳 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MicroserviceHeartbeatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 微服务名称（如 order-service）
     */
    private String serviceName;

    /**
     * 服务实例ID（如 order-service-pod-001 或 IP:Port）
     */
    private String instanceId;

    /**
     * 服务基础访问地址（如 http://order-service:8080）
     */
    private String baseUrl;

    /**
     * 健康检查端点路径（默认 /actuator/health）
     */
    private String healthPath;

    /**
     * 指标端点路径（默认 /actuator/prometheus）
     */
    private String metricsPath;

    /**
     * 实例元数据/标签（如 env=prod, zone=shanghai, version=v1.2.0）
     */
    private Map<String, String> tags;

    /**
     * 关键依赖中间件/下游列表（如 ["mysql", "redis", "order-cancel-queue"]）
     */
    private String dependencies;

}
