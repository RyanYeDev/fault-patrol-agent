package cn.faultpatrol.domain.agent.service.remediation;

import cn.faultpatrol.domain.agent.model.valobj.RemediationActionVO;

import java.util.List;

/**
 * 故障处置动作与人机协同（HITL）服务接口
 */
public interface IRemediationService {

    /**
     * 从诊断报告的处置建议中提取结构化处置动作并持久化
     */
    List<RemediationActionVO> extractAndSaveActions(String sessionId, String remediationText);

    /**
     * 审批通过处置动作
     */
    boolean approveAction(String actionId, String approver, String comment);

    /**
     * 驳回处置动作
     */
    boolean rejectAction(String actionId, String approver, String comment);

    /**
     * 执行处置动作（支持 Dry-Run 安全模式）
     */
    RemediationActionVO executeAction(String actionId, boolean dryRun);

    /**
     * 根据诊断会话ID查询关联的全部处置动作
     */
    List<RemediationActionVO> queryBySessionId(String sessionId);

    /**
     * 查询待审批的处置动作列表
     */
    List<RemediationActionVO> queryPendingActions();

    /**
     * 根据动作ID查询详情
     */
    RemediationActionVO queryByActionId(String actionId);

}
