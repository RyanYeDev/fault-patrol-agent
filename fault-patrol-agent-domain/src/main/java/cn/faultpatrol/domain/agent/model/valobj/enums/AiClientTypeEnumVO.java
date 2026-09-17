package cn.faultpatrol.domain.agent.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 智能体流程节点客户端类型枚举
 * <p>
 * code 值落库在 ai_agent_flow_config.client_type 字段，由装配阶段按客户端类型
 * 组装执行链各阶段的对话客户端。
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AiClientTypeEnumVO {

    DEFAULT("DEFAULT", "通用的"),

    // 诊断策略（Plan-and-Execute 四阶段）
    PLAN_CLIENT("PLAN_CLIENT", "故障分析规划"),
    EVIDENCE_CLIENT("EVIDENCE_CLIENT", "多工具取证执行"),
    SUPERVISION_CLIENT("SUPERVISION_CLIENT", "证据质量监督"),
    REPORT_CLIENT("REPORT_CLIENT", "诊断报告生成"),

    // 流程策略（先规划后执行）
    TOOL_MCP_CLIENT("TOOL_MCP_CLIENT", "工具分析"),
    PLANNING_CLIENT("PLANNING_CLIENT", "任务规划"),
    EXECUTOR_CLIENT("EXECUTOR_CLIENT", "任务执行")

    ;

    private String code;
    private String info;

}
