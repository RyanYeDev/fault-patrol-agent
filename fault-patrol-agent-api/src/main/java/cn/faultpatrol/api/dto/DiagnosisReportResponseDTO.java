package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 诊断报告响应 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiagnosisReportResponseDTO implements Serializable {

    /**
     * 报告ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 告警内容
     */
    private String alertContent;

    /**
     * 根因分析
     */
    private String rootCause;

    /**
     * 处置建议
     */
    private String remediation;

    /**
     * 取证过程（证据链）
     */
    private String evidence;

    /**
     * 完整诊断总结
     */
    private String summary;

    /**
     * 诊断状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

}
