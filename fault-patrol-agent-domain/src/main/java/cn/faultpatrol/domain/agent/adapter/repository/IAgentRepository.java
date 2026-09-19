package cn.faultpatrol.domain.agent.adapter.repository;

import cn.faultpatrol.domain.agent.model.valobj.*;

import java.util.List;
import java.util.Map;

/**
 * AiAgent 仓储接口
 *
 */
public interface IAgentRepository {

    List<AiClientApiVO> queryAiClientApiVOListByClientIds(List<String> clientIdList);

    List<AiClientModelVO> AiClientModelVOByClientIds(List<String> clientIdList);

    List<AiClientToolMcpVO> AiClientToolMcpVOByClientIds(List<String> clientIdList);

    List<AiClientSystemPromptVO> AiClientSystemPromptVOByClientIds(List<String> clientIdList);

    Map<String, AiClientSystemPromptVO> queryAiClientSystemPromptMapByClientIds(List<String> clientIdList);

    List<AiClientAdvisorVO> AiClientAdvisorVOByClientIds(List<String> clientIdList);

    List<AiClientVO> AiClientVOByClientIds(List<String> clientIdList);

    List<AiClientApiVO> queryAiClientApiVOListByModelIds(List<String> modelIdList);

    List<AiClientModelVO> AiClientModelVOByModelIds(List<String> modelIdList);

    Map<String, AiAgentClientFlowConfigVO> queryAiAgentClientFlowConfig(String aiAgentId);

    AiAgentVO queryAiAgentByAgentId(String aiAgentId);

    List<AiAgentClientFlowConfigVO> queryAiAgentClientsByAgentId(String aiAgentId);

    List<AiAgentTaskScheduleVO> queryAllValidTaskSchedule();

    List<Long> queryAllInvalidTaskScheduleIds();

    void createTagOrder(AiRagOrderVO aiRagOrderVO);

    /**
     * 查询可用的智能体列表
     * @return 可用的智能体列表
     */
    List<AiAgentVO> queryAvailableAgents();

    List<AiClientApiVO> queryAiClientApiVOListByApiIds(List<String> apiIdList);

    void saveDiagnosisReport(DiagnosisReportVO diagnosisReportVO);

    List<DiagnosisReportVO> queryDiagnosisReportsBySessionId(String sessionId);

    DiagnosisReportVO queryDiagnosisReportById(Long id);

    List<DiagnosisReportVO> queryRecentDiagnosisReports();

    /**
     * 记录告警指纹并返回去重窗口内的命中次数（首次或超窗返回 1）
     */
    int recordAlert(String fingerprint, String alertName, String severity, String source,
                    String sessionId, int windowMinutes);

    /**
     * 删除知识库台账记录
     *
     * @param knowledgeTag 知识标签
     * @param ragName      知识库名称（为空时删除该标签下全部台账）
     */
    void deleteRagOrder(String knowledgeTag, String ragName);

    // 处置动作相关
    void saveRemediationAction(RemediationActionVO actionVO);
    RemediationActionVO queryRemediationActionById(String actionId);
    List<RemediationActionVO> queryRemediationActionsBySessionId(String sessionId);
    List<RemediationActionVO> queryPendingRemediationActions();
    void updateRemediationStatus(String actionId, String status, String approvedBy, String comment);
    void updateRemediationExecution(String actionId, String status, String executionLog);

    // 巡检监控目标相关
    void savePatrolTarget(PatrolTargetVO targetVO);
    PatrolTargetVO queryPatrolTargetById(Long id);
    PatrolTargetVO queryPatrolTargetByServiceName(String serviceName);
    List<PatrolTargetVO> queryActivePatrolTargets();
    List<PatrolTargetVO> queryAllPatrolTargets();
    void updatePatrolTargetCheckStatus(Long id, String checkStatus, String errorMsg);

    // 通知渠道相关
    void saveNotificationChannel(NotificationChannelVO channelVO);
    NotificationChannelVO queryNotificationChannelById(String channelId);
    List<NotificationChannelVO> queryActiveNotificationChannels();
    List<NotificationChannelVO> queryAllNotificationChannels();
    void updateNotificationChannelStatus(String channelId, Integer status);

}
