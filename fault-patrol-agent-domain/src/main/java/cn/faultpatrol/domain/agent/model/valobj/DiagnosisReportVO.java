package cn.faultpatrol.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 诊断报告值对象
 * <p>
 * 一次诊断任务的最终产物：含根因分析与处置建议的结构化报告，
 * 以及完整的取证过程记录，落库供事后审计与知识沉淀。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiagnosisReportVO {

    /**
     * 报告ID
     */
    private Long id;

    /**
     * 会话ID（一次诊断任务唯一标识）
     */
    private String sessionId;

    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 告警内容（原始诊断输入）
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
     * 诊断状态：COMPLETED（完成）/ STEP_LIMIT（达到最大步数）
     */
    private String status;

    /**
     * 创建时间
     */
    private java.util.Date createTime;

    /**
     * 更新时间
     */
    private java.util.Date updateTime;

}
