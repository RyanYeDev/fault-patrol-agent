package cn.faultpatrol.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警通知渠道类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationChannelTypeEnum {

    DINGTALK("DINGTALK", "钉钉机器人"),
    FEISHU("FEISHU", "飞书机器人"),
    WECOM("WECOM", "企业微信机器人"),
    SLACK("SLACK", "Slack Incoming Webhook"),
    GENERIC_WEBHOOK("GENERIC_WEBHOOK", "通用 HTTP Webhook");

    private final String code;
    private final String description;

    public static NotificationChannelTypeEnum fromCode(String code) {
        if (code == null) return GENERIC_WEBHOOK;
        for (NotificationChannelTypeEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return GENERIC_WEBHOOK;
    }

}
