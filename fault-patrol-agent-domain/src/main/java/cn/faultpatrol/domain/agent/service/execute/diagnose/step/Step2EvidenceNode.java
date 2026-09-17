package cn.faultpatrol.domain.agent.service.execute.diagnose.step;

import cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiClientTypeEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.support.ToolTraceSupport;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.factory.DefaultDiagnoseAgentExecuteStrategyFactory;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.support.SectionParser;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        ensureNotCancelled(requestParameter.getSessionId());
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

        // 清理上轮残留的工具轨迹
        ToolTraceSupport.drain();

        // Agent 级知识标签：按业务域召回对应故障手册（RagAnswerAdvisor 读取 qa_filter_expression）
        String knowledgeTag = dynamicContext.getValue("agentKnowledgeTag");
        final String knowledgeFilter = (knowledgeTag == null || knowledgeTag.isBlank())
                ? null : "knowledge == '" + knowledgeTag + "'";

        String evidenceResult = chatClient
                .prompt(evidencePrompt)
                .advisors(a -> {
                    a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024);
                    if (knowledgeFilter != null) {
                        a.param("qa_filter_expression", knowledgeFilter);
                    }
                })
                .call().content();

        assert evidenceResult != null;

        // 取走本轮工具调用轨迹（结构化证据留痕，报告落库时持久化）
        List<Map<String, Object>> toolTrace = ToolTraceSupport.drain();
        if (!toolTrace.isEmpty()) {
            log.info("本轮取证调用巡检工具 {} 次", toolTrace.size());
        }
        // 跨轮次累计全部工具调用轨迹
        List<Map<String, Object>> accumulatedTrace = dynamicContext.getValue("toolTraceAcc");
        if (accumulatedTrace == null) {
            accumulatedTrace = new ArrayList<>();
        }
        accumulatedTrace.addAll(toolTrace);
        dynamicContext.setValue("toolTraceAcc", accumulatedTrace);

        parseEvidenceResult(dynamicContext, evidenceResult, requestParameter.getSessionId());

        // 将取证结果保存到动态上下文中，供下一步使用
        dynamicContext.setValue("evidenceResult", evidenceResult);

        // 更新执行历史（附工具调用摘要，供监督节点与报告参考）
        String toolSummary = toolTrace.stream()
                .map(t -> t.get("tool") + "(" + t.get("costMs") + "ms" + (t.get("error") != null ? ",失败" : "") + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");
        String stepSummary = String.format("""
                === 第 %d 步执行记录 ===
                【规划阶段】%s
                【取证阶段】%s
                【工具调用】%s
                """, dynamicContext.getStep(), planResult, evidenceResult, toolSummary);

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

        Map<String, String> sections = SectionParser.parse(evidenceResult, List.of("取证目标", "取证过程", "取证结果", "证据检查"));
        if (sections.isEmpty()) {
            // 降级：模型未按模板输出时，整段内容作为取证结果事件发送
            log.warn("取证输出未匹配到分节模板，整段降级发送");
            sendEvidenceSubResult(dynamicContext, "evidence_result", evidenceResult, sessionId);
            return;
        }

        Map<String, String> subTypeMap = Map.of(
                "取证目标", "evidence_target",
                "取证过程", "evidence_process",
                "取证结果", "evidence_result",
                "证据检查", "evidence_quality");

        sections.forEach((section, content) ->
                sendEvidenceSubResult(dynamicContext, subTypeMap.getOrDefault(section, section), content, sessionId));
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
