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
 * 阶段三：质量监督节点
 * <p>
 * 对取证结果做质量监督：检查证据是否充分、根因方向是否收敛。
 * PASS 通过进入报告阶段；FAIL/OPTIMIZE 则改写当前任务并回环到规划节点重新取证。
 */
@Slf4j
@Service
public class Step3SupervisionNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 第三阶段：质量监督
        log.info("阶段3: 证据质量监督");

        // 从动态上下文中获取取证结果
        String evidenceResult = dynamicContext.getValue("evidenceResult");
        if (evidenceResult == null || evidenceResult.trim().isEmpty()) {
            log.warn("取证结果为空，跳过质量监督");
            return "质量监督跳过";
        }

        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.SUPERVISION_CLIENT.getCode());

        String supervisionPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(), requestParameter.getMessage(), evidenceResult);

        // 获取对话客户端
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String supervisionResult = chatClient
                .prompt(supervisionPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert supervisionResult != null;
        parseSupervisionResult(dynamicContext, supervisionResult, requestParameter.getSessionId());

        // 将监督结果保存到动态上下文中
        dynamicContext.setValue("supervisionResult", supervisionResult);

        // 根据监督结果决定是否需要重新取证
        if (supervisionResult.contains("是否通过: FAIL")) {
            log.info("质量检查未通过，需要重新取证");
            dynamicContext.setCurrentTask("根据质量监督的建议重新取证");
        } else if (supervisionResult.contains("是否通过: OPTIMIZE")) {
            log.info("质量检查建议优化，继续改进");
            dynamicContext.setCurrentTask("根据质量监督的建议优化取证");
        } else {
            log.info("质量检查通过");
            dynamicContext.setCompleted(true);
        }

        // 更新执行历史
        String stepSummary = String.format("""
                === 第 %d 步完整记录 ===
                【规划阶段】%s
                【取证阶段】%s
                【监督阶段】%s
                """, dynamicContext.getStep(),
                dynamicContext.getValue("planResult"),
                evidenceResult,
                supervisionResult);

        dynamicContext.getExecutionHistory().append(stepSummary);

        // 增加步骤计数
        dynamicContext.setStep(dynamicContext.getStep() + 1);

        // 如果任务已完成或达到最大步数，进入报告阶段；否则回环到规划节点
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 如果任务已完成或达到最大步数，进入报告阶段
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return getBean("step4ReportNode");
        }

        // 否则返回到Step1PlanningNode进行下一轮规划
        return getBean("step1PlanningNode");
    }

    /**
     * 解析监督结果
     */
    private void parseSupervisionResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext, String supervisionResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("=== 第 {} 步监督结果 ===", step);

        String[] lines = supervisionResult.split("\n");
        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("质量评估:")) {
                sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "assessment";
                sectionContent.setLength(0);
                continue;
            } else if (line.contains("问题识别:")) {
                sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "issues";
                sectionContent.setLength(0);
                continue;
            } else if (line.contains("改进建议:")) {
                sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "suggestions";
                sectionContent.setLength(0);
                continue;
            } else if (line.contains("质量评分:")) {
                sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "score";
                sectionContent.setLength(0);
                sectionContent.append(line.substring(line.indexOf(":") + 1).trim());
                continue;
            } else if (line.contains("是否通过:")) {
                sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "pass";
                sectionContent.setLength(0);
                sectionContent.append(line.substring(line.indexOf(":") + 1).trim());
                continue;
            }

            // 收集当前部分的内容
            if (!currentSection.isEmpty()) {
                if (sectionContent.length() > 0) {
                    sectionContent.append("\n");
                }
                sectionContent.append(line);
            }
        }

        // 发送最后一个部分的内容
        sendSupervisionSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);

        // 发送完整的监督结果
        sendSupervisionResult(dynamicContext, supervisionResult, sessionId);
    }

    /**
     * 发送监督结果到流式输出
     */
    private void sendSupervisionResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String supervisionResult, String sessionId) {
        DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createSupervisionResult(
                dynamicContext.getStep(), supervisionResult, sessionId);
        sendSseResult(dynamicContext, result);
    }

    /**
     * 发送监督子结果到流式输出（细粒度标识）
     */
    private void sendSupervisionSubResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                          String section, String content, String sessionId) {
        if (!content.isEmpty() && !section.isEmpty()) {
            DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createSupervisionSubResult(
                    dynamicContext.getStep(), section, content, sessionId);
            sendSseResult(dynamicContext, result);
        }
    }

}
