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
 * 阶段一：规划节点（故障分析）
 * <p>
 * 对告警进行故障分析，结合执行历史评估当前进度，输出取证策略
 * （需要调用的指标 / 链路 / 业务数据），并评估完成度决定是否继续取证。
 */
@Slf4j
@Service
public class Step1PlanningNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("=== 执行第 {} 步 ===", dynamicContext.getStep());

        // 获取配置信息
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.PLAN_CLIENT.getCode());

        // 第一阶段：故障分析规划
        log.info("阶段1: 故障分析规划");
        String planPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                requestParameter.getMessage(),
                dynamicContext.getStep(),
                dynamicContext.getMaxStep(),
                !dynamicContext.getExecutionHistory().isEmpty() ? dynamicContext.getExecutionHistory().toString() : "[首次执行]",
                dynamicContext.getCurrentTask()
        );

        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String planResult = chatClient
                .prompt(planPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert planResult != null;
        parsePlanResult(dynamicContext, planResult, requestParameter.getSessionId());

        // 将分析结果保存到动态上下文中，供下一步使用
        dynamicContext.setValue("planResult", planResult);

        // 检查是否已完成
        if (planResult.contains("任务状态: COMPLETED") ||
                planResult.contains("完成度评估: 100%")) {
            dynamicContext.setCompleted(true);
            log.info("故障分析显示已完成！");
            return router(requestParameter, dynamicContext);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 如果任务已完成或达到最大步数，进入报告阶段
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return getBean("step4ReportNode");
        }

        // 否则进入取证执行阶段
        return getBean("step2EvidenceNode");
    }

    private void parsePlanResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext, String planResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("=== 第 {} 步规划结果 ===", step);

        String[] lines = planResult.split("\n");
        String currentSection = "";
        StringBuilder sectionContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("故障分析:")) {
                sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "plan_status";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("执行历史评估:")) {
                sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "plan_history";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("取证策略:")) {
                sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "plan_strategy";
                sectionContent = new StringBuilder();
                continue;
            } else if (line.contains("完成度评估:")) {
                sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "plan_progress";
                sectionContent = new StringBuilder();
                sectionContent.append(line).append("\n");
                continue;
            } else if (line.contains("任务状态:")) {
                sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
                currentSection = "plan_task_status";
                sectionContent = new StringBuilder();
                sectionContent.append(line).append("\n");
                continue;
            }

            // 收集当前section的内容
            if (!currentSection.isEmpty()) {
                sectionContent.append(line).append("\n");
            }
        }

        // 发送最后一个section的内容
        sendPlanSubResult(dynamicContext, currentSection, sectionContent.toString(), sessionId);
    }

    /**
     * 发送规划阶段细分结果到流式输出
     */
    private void sendPlanSubResult(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                   String subType, String content, String sessionId) {
        if (!subType.isEmpty() && !content.isEmpty()) {
            DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createPlanSubResult(
                    dynamicContext.getStep(), subType, content, sessionId);
            sendSseResult(dynamicContext, result);
        }
    }

}
