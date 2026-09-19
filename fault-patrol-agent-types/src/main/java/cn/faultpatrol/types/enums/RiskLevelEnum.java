package cn.faultpatrol.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作风险等级枚举
 */
@Getter
@AllArgsConstructor
public enum RiskLevelEnum {

    LOW("LOW", "低风险（只读/安全操作）"),
    MEDIUM("MEDIUM", "中风险（重启/重试/熔断）"),
    HIGH("HIGH", "高风险（清理队列/切换数据源）"),
    CRITICAL("CRITICAL", "极高风险（回滚版本/下线节点）");

    private final String code;
    private final String description;

    public static RiskLevelEnum fromCode(String code) {
        if (code == null) return MEDIUM;
        for (RiskLevelEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return MEDIUM;
    }

}
