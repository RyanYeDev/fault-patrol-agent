package cn.faultpatrol.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 故障处置动作类型枚举
 */
@Getter
@AllArgsConstructor
public enum RemediationActionTypeEnum {

    RESTART_POD("RESTART_POD", "重启容器实例", RiskLevelEnum.MEDIUM),
    SCALE_REPLICAS("SCALE_REPLICAS", "动态扩缩容", RiskLevelEnum.LOW),
    DRAIN_MQ_QUEUE("DRAIN_MQ_QUEUE", "清空/隔离积压队列", RiskLevelEnum.HIGH),
    CLEAR_CACHE("CLEAR_CACHE", "清理异常缓存键", RiskLevelEnum.LOW),
    SWITCH_DATASOURCE("SWITCH_DATASOURCE", "数据源主从切换/降级", RiskLevelEnum.HIGH),
    CIRCUIT_BREAK("CIRCUIT_BREAK", "熔断下游异常接口", RiskLevelEnum.MEDIUM),
    ROLLBACK_DEPLOYMENT("ROLLBACK_DEPLOYMENT", "回滚应用版本", RiskLevelEnum.CRITICAL),
    MANUAL_INTERVENTION("MANUAL_INTERVENTION", "人工介入操作", RiskLevelEnum.LOW);

    private final String code;
    private final String description;
    private final RiskLevelEnum defaultRisk;

    public static RemediationActionTypeEnum fromCode(String code) {
        if (code == null) return MANUAL_INTERVENTION;
        for (RemediationActionTypeEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return MANUAL_INTERVENTION;
    }

}
