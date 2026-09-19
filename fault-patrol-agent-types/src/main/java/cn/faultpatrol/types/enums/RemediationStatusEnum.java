package cn.faultpatrol.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 故障处置动作状态枚举
 */
@Getter
@AllArgsConstructor
public enum RemediationStatusEnum {

    PROPOSED("PROPOSED", "方案已建议（待审批）"),
    APPROVED("APPROVED", "审批已通过（排队执行）"),
    REJECTED("REJECTED", "审批已驳回"),
    EXECUTING("EXECUTING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败"),
    ROLLED_BACK("ROLLED_BACK", "已回滚");

    private final String code;
    private final String description;

    public static RemediationStatusEnum fromCode(String code) {
        if (code == null) return PROPOSED;
        for (RemediationStatusEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return PROPOSED;
    }

}
