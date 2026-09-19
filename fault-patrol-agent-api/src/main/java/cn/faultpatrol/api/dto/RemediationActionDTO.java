package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 故障处置动作 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemediationActionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处置动作唯一ID
     */
    private String actionId;

    /**
     * 关联的诊断会话ID
     */
    private String sessionId;

    /**
     * 动作标题/简述
     */
    private String title;

    /**
     * 动作类型：RESTART_POD / SCALE_REPLICAS / DRAIN_MQ_QUEUE / CLEAR_CACHE / SWITCH_DATASOURCE / CIRCUIT_BREAK / ROLLBACK_DEPLOYMENT / MANUAL_INTERVENTION
     */
    private String actionType;

    /**
     * 目标资源（如 服务名、Pod名、队列名、缓存键）
     */
    private String targetResource;

    /**
     * 风险等级：LOW / MEDIUM / HIGH / CRITICAL
     */
    private String riskLevel;

    /**
     * 执行指令或Payload（如 kubectl 重启命令、Redis DEL 命令、MQ purge 命令）
     */
    private String command;

    /**
     * 回滚/补偿预案
     */
    private String rollbackPlan;

    /**
     * 当前状态：PROPOSED / APPROVED / REJECTED / EXECUTING / SUCCESS / FAILED / ROLLED_BACK
     */
    private String status;

    /**
     * 审批意见/驳回原因
     */
    private String approvalComment;

    /**
     * 审批人
     */
    private String approvedBy;

    /**
     * 执行日志/输出结果
     */
    private String executionLog;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
