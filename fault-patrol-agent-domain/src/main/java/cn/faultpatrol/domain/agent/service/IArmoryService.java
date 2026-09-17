package cn.faultpatrol.domain.agent.service;

import cn.faultpatrol.domain.agent.model.valobj.AiAgentVO;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;

import java.util.List;

/**
 * 装配服务接口
 * <p>
 * 提供智能体组件装配（API → MCP → Model → Advisor → Client）与诊断报告查询能力。
 */
public interface IArmoryService {

    /**
     * 装配所有可用智能体
     *
     * @return 可用智能体列表
     */
    List<AiAgentVO> acceptArmoryAllAvailableAgents();

    /**
     * 按智能体ID装配
     *
     * @param agentId 智能体ID
     */
    void acceptArmoryAgent(String agentId);

    /**
     * 查询可用智能体列表
     *
     * @return 智能体列表
     */
    List<AiAgentVO> queryAvailableAgents();

    /**
     * 按API ID装配
     *
     * @param apiId API ID
     */
    void acceptArmoryAgentClientModelApi(String apiId);

    /**
     * 按会话ID查询诊断报告
     *
     * @param sessionId 会话ID
     * @return 诊断报告列表
     */
    List<DiagnosisReportVO> queryDiagnosisReportsBySessionId(String sessionId);

    /**
     * 按ID查询诊断报告
     *
     * @param id 报告ID
     * @return 诊断报告
     */
    DiagnosisReportVO queryDiagnosisReportById(Long id);

}
