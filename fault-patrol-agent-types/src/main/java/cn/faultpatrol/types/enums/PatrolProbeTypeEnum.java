package cn.faultpatrol.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 巡检探针类型枚举
 */
@Getter
@AllArgsConstructor
public enum PatrolProbeTypeEnum {

    HTTP_HEALTH("HTTP_HEALTH", "HTTP/Actuator 探针"),
    PROMETHEUS_METRIC("PROMETHEUS_METRIC", "Prometheus 指标水位探针"),
    REDIS_HEALTH("REDIS_HEALTH", "Redis 缓存健康探针"),
    RABBITMQ_QUEUE("RABBITMQ_QUEUE", "RabbitMQ 队列积压探针");

    private final String code;
    private final String description;

    public static PatrolProbeTypeEnum fromCode(String code) {
        if (code == null) return HTTP_HEALTH;
        for (PatrolProbeTypeEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return HTTP_HEALTH;
    }

}
