package cn.faultpatrol.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 诊断报告表 PO 对象
 * <p>
 * 一次巡检诊断任务的最终产物：根因分析、处置建议与取证过程记录。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiagnosisReport {

    /**
     * 主键ID
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
     * 工具调用轨迹（结构化 JSON）
     */
    private String toolTrace;

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
