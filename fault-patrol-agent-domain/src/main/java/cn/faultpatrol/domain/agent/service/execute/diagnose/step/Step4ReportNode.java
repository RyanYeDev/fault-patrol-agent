package cn.faultpatrol.domain.agent.service.execute.diagnose.step;

import cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiClientTypeEnumVO;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.factory.DefaultDiagnoseAgentExecuteStrategyFactory;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.support.ReportSectionExtractor;
import cn.faultpatrol.domain.agent.service.remediation.IRemediationService;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 阶段四：诊断报告节点
 * <p>
 * 汇总取证过程，生成含故障概述、根因分析与处置建议的结构化诊断报告，
 * 逐 token 流式输出到 SSE，并将报告落库供审计查询。
 */
@Slf4j
@Service
public class Step4ReportNode extends AbstractExecuteSupport {

    @Resource
    private IRemediationService remediationService;

    @Resource
    private cn.faultpatrol.domain.agent.service.notify.INotificationService notificationService;

    @Resource
    private cn.faultpatrol.domain.agent.service.learning.IPostmortemService postmortemService;

    /** 流式报告输出为空时的兜底文案 */
    private static final String FALLBACK_PREFIX = "## 诊断报告（兜底输出）\n\n> 模型流式报告生成失败";

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        ensureNotCancelled(requestParameter.getSessionId());
        log.info("=== 执行第 {} 步 ===", dynamicContext.getStep());

        // 第四阶段：诊断报告
        log.info("阶段4: 诊断报告生成");

        // 生成最终诊断报告（无论任务是否完成都需要生成，改为逐 token 流式输出）
        generateFinalReport(requestParameter, dynamicContext);

        // 持久化诊断报告
        persistDiagnosisReport(requestParameter, dynamicContext);

        log.info("=== 巡检诊断执行结束 ====");

        return "diagnosis report completed!";
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 报告节点是最后一个节点，返回null表示执行结束
        return defaultStrategyHandler;
    }

    /**
     * 生成最终诊断报告 —— 使用 stream() 逐 token 流式输出到 SSE，
     * 前端将同一段 delta 增量追加到同一个气泡，形成打字机效果。
     */
    private void generateFinalReport(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        boolean isCompleted = dynamicContext.isCompleted();
        String sessionId = requestParameter.getSessionId();
        try {
            log.info("--- 开始流式生成{}诊断报告 ---", isCompleted ? "已完成" : "未完成");

            AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.REPORT_CLIENT.getCode());
            if (aiAgentClientFlowConfigVO == null) {
                log.warn("未配置 REPORT_CLIENT 客户端，使用兜底输出");
                fallbackSummary(dynamicContext, "未配置报告客户端", sessionId);
                return;
            }

            String reportPrompt = getReportPrompt(aiAgentClientFlowConfigVO, requestParameter, dynamicContext, isCompleted);
            ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

            Flux<String> flux = chatClient
                    .prompt(reportPrompt)
                    .advisors(a -> a
                            .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId + "-report")
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                    .stream()
                    .content();

            StringBuilder full = new StringBuilder();
            boolean anyToken = false;
            try {
                for (String token : flux.toIterable()) {
                    if (token == null || token.isEmpty()) {
                        continue;
                    }
                    anyToken = true;
                    full.append(token);
                    sendReportStreamToken(dynamicContext, token, sessionId);
                }
            } catch (Exception streamEx) {
                // 流式中途异常：已发出的部分保留；一条都没发出才走兜底
                if (streamEx instanceof org.springframework.web.reactive.function.client.WebClientResponseException wce) {
                    log.error("报告流式输出中断：HTTP {}, body={}", wce.getStatusCode().value(), wce.getResponseBodyAsString());
                } else {
                    log.error("报告流式输出中断：{}", streamEx.getMessage(), streamEx);
                }
            }

            if (!anyToken) {
                log.warn("报告流式输出没有任何内容，走兜底输出");
                fallbackSummary(dynamicContext, "模型流式输出为空", sessionId);
            } else {
                dynamicContext.setValue("finalReport", full.toString());
                log.info("报告流式输出完成，共 {} 字符", full.length());
            }
        } catch (Exception e) {
            log.error("生成最终诊断报告时出现异常: {}", e.getMessage(), e);
            fallbackSummary(dynamicContext, e.getMessage(), sessionId);
        }
    }

    /**
     * 将诊断报告持久化到数据库
     */
    private void persistDiagnosisReport(ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        try {
            String finalReport = dynamicContext.getValue("finalReport");
            String summary = (finalReport == null || finalReport.isBlank())
                    ? dynamicContext.getExecutionHistory().toString()
                    : finalReport;

            StringBuilder history = dynamicContext.getExecutionHistory();

            // 结构化工具调用轨迹（跨轮次累计）
            String toolTraceJson = "[]";
            List<Map<String, Object>> accumulatedTrace = dynamicContext.getValue("toolTraceAcc");
            if (accumulatedTrace != null && !accumulatedTrace.isEmpty()) {
                try {
                    toolTraceJson = com.alibaba.fastjson.JSON.toJSONString(accumulatedTrace);
                } catch (Exception e) {
                    log.warn("工具调用轨迹序列化失败：{}", e.getMessage());
                }
            }

            DiagnosisReportVO reportVO = DiagnosisReportVO.builder()
                    .sessionId(requestParameter.getSessionId())
                    .agentId(requestParameter.getAiAgentId())
                    .alertContent(requestParameter.getMessage())
                    .rootCause(extractSection(summary, "根因分析"))
                    .remediation(extractSection(summary, "处置建议"))
                    .evidence(history == null ? "" : history.toString())
                    .toolTrace(toolTraceJson)
                    .summary(truncate(summary, 20000))
                    .status(dynamicContext.isCompleted() ? "COMPLETED" : "STEP_LIMIT")
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();

            repository.saveDiagnosisReport(reportVO);
            log.info("诊断报告已落库，会话ID：{}", requestParameter.getSessionId());

            // 自动提取结构化处置动作供 HITL 审批流转
            if (StringUtils.isNotBlank(reportVO.getRemediation())) {
                try {
                    remediationService.extractAndSaveActions(requestParameter.getSessionId(), reportVO.getRemediation());
                } catch (Exception ex) {
                    log.warn("提取结构化处置动作失败（不影响主诊断流程）：{}", ex.getMessage());
                }
            }

            // 多渠道通知广播（钉钉 / 飞书 / 企业微信 / Slack / Webhook）
            try {
                notificationService.sendDiagnosisReportNotification(reportVO);
            } catch (Exception ex) {
                log.warn("广播诊断报告通知异常（不影响主诊断流程）：{}", ex.getMessage());
            }

            // 自进化学习闭环：将实战成功排查经验自动沉淀为知识库 Playbook
            try {
                postmortemService.learnAndSynthesizePlaybook(reportVO);
            } catch (Exception ex) {
                log.warn("自主学习沉淀实战手册异常（不影响主诊断流程）：{}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("诊断报告落库失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 从报告文本中提取指定小节内容（报告模板：## 根因分析 / ## 处置建议）
     */
    private String extractSection(String report, String sectionName) {
        return ReportSectionExtractor.extractSection(report, sectionName);
    }

    private String truncate(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() > maxLength) {
            return content.substring(0, maxLength) + "\n\n...(内容过长已截断)";
        }
        return content;
    }

    /**
     * 发送报告阶段单条 token 增量到流式输出
     */
    private void sendReportStreamToken(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String token, String sessionId) {
        DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createReportStreamResult(token, sessionId);
        sendSseResult(dynamicContext, result);
    }

    /**
     * 报告失败兜底：把已收集的取证过程作为报告内容发出，保证结果不为空
     */
    private void fallbackSummary(DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext, String reason, String sessionId) {
        try {
            StringBuilder history = dynamicContext.getExecutionHistory();
            String historyText = history == null ? "" : history.toString();
            String reasonText = (reason == null || reason.isBlank()) ? "未知原因" : reason;

            String content;
            if (!historyText.isBlank()) {
                content = FALLBACK_PREFIX + "\n\n> " + reasonText + "\n\n以下为本次诊断过程的完整记录：\n\n" + historyText;
            } else {
                content = "抱歉，本次未能生成诊断报告（" + reasonText + "），请查看诊断过程或重试。";
            }
            content = truncate(content, 20000);
            DiagnoseExecuteResultEntity result = DiagnoseExecuteResultEntity.createReportResult(content, sessionId);
            sendSseResult(dynamicContext, result);
            dynamicContext.setValue("finalReport", content);
            log.info("已发送兜底报告内容，长度 {}", content.length());
        } catch (Exception e) {
            log.error("兜底报告输出失败：{}", e.getMessage(), e);
        }
    }

    private static String getReportPrompt(AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO, ExecuteCommandEntity requestParameter, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext, boolean isCompleted) {
        String reportPrompt;
        if (isCompleted) {
            reportPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                    requestParameter.getMessage(),
                    dynamicContext.getExecutionHistory().toString());
        } else {
            reportPrompt = String.format("""
                    虽然诊断任务未完全执行完成，但请基于已有的取证过程，尽力输出故障诊断报告：

                    **原始告警:** %s

                    **已执行的取证过程和获得的证据:**
                    %s

                    **要求:**
                    1. 基于已有证据，尽力定位故障根因
                    2. 如果证据不足，说明哪些部分无法确认并给出原因
                    3. 提供已能确定的部分结论
                    4. 给出完成剩余取证的具体建议
                    5. 输出结构化诊断报告：## 故障概述 / ## 根因分析 / ## 处置建议 / ## 预防措施
                    """,
                    requestParameter.getMessage(),
                    dynamicContext.getExecutionHistory().toString());
        }
        return reportPrompt;
    }

}
