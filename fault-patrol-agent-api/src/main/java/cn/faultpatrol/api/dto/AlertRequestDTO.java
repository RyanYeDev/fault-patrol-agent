package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 告警接入请求 DTO
 * <p>
 * 告警源（监控平台 / 自研告警中心）通过 webhook 推送告警，
 * 平台自动发起一次巡检诊断任务并以 SSE 流式返回诊断过程。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertRequestDTO implements Serializable {

    /**
     * 智能体ID（缺省时使用配置的默认巡检智能体）
     */
    private String aiAgentId;

    /**
     * 告警名称
     */
    private String alertName;

    /**
     * 告警级别：critical / warning / info
     */
    private String severity;

    /**
     * 告警内容（含服务名、指标、阈值等原始告警信息）
     */
    private String alertContent;

    /**
     * 告警来源（如 prometheus / cloud-monitor）
     */
    private String source;

}
