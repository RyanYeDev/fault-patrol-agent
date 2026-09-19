package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 巡检监控目标微服务 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatrolTargetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 目标服务名
     */
    private String serviceName;

    /**
     * 探针类型：HTTP_HEALTH / PROMETHEUS_METRIC / REDIS_HEALTH / RABBITMQ_QUEUE
     */
    private String probeType;

    /**
     * 探针端点/表达式（如 http://order-service:8080/actuator/health 或 rate(http_requests_total{status=~"5.."}[5m])）
     */
    private String targetEndpoint;

    /**
     * 异常判定阈值配置（JSON 格式，如 {"status": "UP", "errorRateThreshold": 0.05, "latencyThresholdMs": 1000}）
     */
    private String thresholdConfig;

    /**
     * 巡检周期 Cron 或间隔秒数
     */
    private String intervalCron;

    /**
     * 触发巡检绑定的智能体ID
     */
    private String aiAgentId;

    /**
     * 静默窗口/防抖（分钟）
     */
    private Integer quietWindowMinutes;

    /**
     * 状态：1 启用，0 停用
     */
    private Integer status;

    /**
     * 最近巡检时间
     */
    private Date lastCheckTime;

    /**
     * 最近巡检结果状态（HEALTHY / DEGRADED / UNHEALTHY）
     */
    private String lastCheckStatus;

    /**
     * 最近巡检异常信息
     */
    private String lastErrorMsg;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
