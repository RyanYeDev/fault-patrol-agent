package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警与诊断报告通知渠道配置 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationChannelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 渠道唯一标识
     */
    private String channelId;

    /**
     * 渠道名称（如 SRE-故障告警群）
     */
    private String channelName;

    /**
     * 渠道类型：DINGTALK / FEISHU / WECOM / SLACK / GENERIC_WEBHOOK
     */
    private String channelType;

    /**
     * Webhook 请求地址
     */
    private String webhookUrl;

    /**
     * 签名校验秘钥（钉钉加签 / 飞书加签）
     */
    private String secret;

    /**
     * 是否启用（1: 启用, 0: 禁用）
     */
    private Integer status;

    /**
     * 关注的告警级别（如 critical,warning）
     */
    private String notifySeverities;

    /**
     * 创建时间
     */
    private Date createTime;

}
