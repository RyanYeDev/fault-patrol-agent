package cn.faultpatrol.domain.agent.service.execute.diagnose.step;

import cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiClientTypeEnumVO;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.factory.DefaultDiagnoseAgentExecuteStrategyFactory;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 阶段二：取证执行节点
 * <p>
 * 按规划节点给出的取证策略，调用该客户端绑定的 MCP 巡检工具
 * （业务数据 / Redis / MQ / 容器 / Prometheus / Jaeger）进行多工具协同取证，
 * 输出取证目标、过程、结果与证据检查。
 */
@Slf4j
@Service
public class Step2EvidenceNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("阶段2: 多工具取证执行");

        // 从动态上下文中获取规划结果
        String planResult = dynamicContext.getValue("planResult");
        if (planResult == null || planResult.trim().isEmpty()) {
            log.warn("规划结果为空，使用默认取证策略");
            planResult = "按告警内容执行取证";
        }

        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.EVIDENCE_CLIENT.getCode());

        String evidencePrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(), requestParameter.getMessage(), planResult);

        // 获取对话客户端（该客户端绑定了 MCP 巡检工具回调，模型可自主调用工具取证）
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String evidenceResult = chatClient
                .prompt(evidencePrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert evidenceResult != null;
        parseEvidenceResult(dynamicContext, evidenceResult, requestParameter.getSessionId());

        // 将取证结果保存到动态上下文中，供下一步使用
        dynamicContext.setValue("evidenceResult", evidenceResult);

        // 更新执行历史
        String stepSummary = String.format("""
                === 第 %d 步执行记录 ===
                【规划阶段】%s
                【取证阶段】%s
                """, dynamicContext.getStep(), planResult, evidenceResult);

        dynamicContext.getExecutionHistory().append(stepSummary);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return getBean("step3SupervisionNode");
    }

    /**
     * 解析取证结果
     */
    private void parseEvidenceResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext, String evidenceResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("=== 第 {} 步取证结果 ===", step);

        String[] lines = evidenceResult.split("\n");
        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("取证目标:")) {
                sendEvidenceSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "evidence_target";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("取证过程:")) {
                sendEvidenceSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "evidence_process";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("取证结果:")) {
                sendEvidenceSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "evidence_result";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("证据检查:")) {
                sendEvidenceSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "evidence_quality";
                sectionContent = new StringBuilder();
                continue;
            }

            // 收集当前section的内容
            if (!currentSection.isEmpty()) {
                sectionContent.append(line).append("\n");
            }
        }

        // 发送最后一个section的内容
        sendEvidenceSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
    }

    /**
     * 发送取证执行阶段细分结果到流式输出
     */
    private void sendEvidenceSubResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String subType, String content, String sessionId) {
        if (!subType.isEmpty() && !content.isEmpty()) {
            DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createEvidenceSubResult(
                    dynamicContext.getStep(), subType, content, sessionId);
            sendSseResult(dynamicContext, result);
        }
    }

}
