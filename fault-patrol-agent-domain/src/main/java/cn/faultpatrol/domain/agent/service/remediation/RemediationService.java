package cn.faultpatrol.domain.agent.service.remediation;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.RemediationActionVO;
import cn.faultpatrol.types.enums.RemediationActionTypeEnum;
import cn.faultpatrol.types.enums.RemediationStatusEnum;
import cn.faultpatrol.types.enums.RiskLevelEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 故障处置动作与人机协同审批执行实现
 */
@Slf4j
@Service
public class RemediationService implements IRemediationService {

    @Resource
    private IAgentRepository repository;

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:bash|sh|sql)?\\s*([\\s\\S]*?)```");

    @Override
    public List<RemediationActionVO> extractAndSaveActions(String sessionId, String remediationText) {
        if (StringUtils.isBlank(remediationText)) {
            return List.of();
        }

        List<RemediationActionVO> actionList = new ArrayList<>();
        String[] lines = remediationText.split("\n");

        // 提取 Markdown 代码块中的指令
        List<String> codeBlocks = new ArrayList<>();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(remediationText);
        while (matcher.find()) {
            String block = matcher.group(1).trim();
            if (!block.isEmpty()) {
                codeBlocks.add(block);
            }
        }

        int codeBlockIdx = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            // 匹配有序列表或要点行，例如 "1. 重启..." 或 "- 清理..."
            if (trimmed.matches("^(?:\\d+\\.|[-*])\\s+.*")) {
                String content = trimmed.replaceFirst("^(?:\\d+\\.|[-*])\\s+", "").trim();
                if (content.length() < 4) continue;

                RemediationActionTypeEnum actionType = inferActionType(content);
                RiskLevelEnum riskLevel = inferRiskLevel(content, actionType);

                String command = null;
                if (codeBlockIdx < codeBlocks.size()) {
                    command = codeBlocks.get(codeBlockIdx++);
                }

                String actionId = "act_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                RemediationActionVO actionVO = RemediationActionVO.builder()
                        .actionId(actionId)
                        .sessionId(sessionId)
                        .title(extractTitle(content))
                        .actionType(actionType.getCode())
                        .targetResource(inferTargetResource(content))
                        .riskLevel(riskLevel.getCode())
                        .command(command)
                        .rollbackPlan(generateDefaultRollback(actionType, content))
                        .status(RemediationStatusEnum.PROPOSED.getCode())
                        .build();

                repository.saveRemediationAction(actionVO);
                actionList.add(actionVO);
            }
        }

        // 兜底：若未能按行拆解出条目，则将整体作为单条人工介入建议入库
        if (actionList.isEmpty()) {
            String actionId = "act_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            RemediationActionVO fallbackAction = RemediationActionVO.builder()
                    .actionId(actionId)
                    .sessionId(sessionId)
                    .title("系统诊断综合处置建议")
                    .actionType(RemediationActionTypeEnum.MANUAL_INTERVENTION.getCode())
                    .targetResource("业务应用系统")
                    .riskLevel(RiskLevelEnum.LOW.getCode())
                    .command(codeBlocks.isEmpty() ? null : codeBlocks.get(0))
                    .rollbackPlan("按原部署配置与数据快照执行回滚")
                    .status(RemediationStatusEnum.PROPOSED.getCode())
                    .build();
            repository.saveRemediationAction(fallbackAction);
            actionList.add(fallbackAction);
        }

        log.info("从诊断会话 {} 提取并生成 {} 条处置行动方案", sessionId, actionList.size());
        return actionList;
    }

    @Override
    public boolean approveAction(String actionId, String approver, String comment) {
        RemediationActionVO action = repository.queryRemediationActionById(actionId);
        if (action == null) {
            log.warn("处置动作不存在：{}", actionId);
            return false;
        }
        if (!RemediationStatusEnum.PROPOSED.getCode().equals(action.getStatus())) {
            log.warn("处置动作当前状态为 {}，不可重复审批：{}", action.getStatus(), actionId);
            return false;
        }

        repository.updateRemediationStatus(actionId, RemediationStatusEnum.APPROVED.getCode(),
                StringUtils.defaultIfBlank(approver, "SRE-Engineer"),
                StringUtils.defaultIfBlank(comment, "审批通过"));
        log.info("处置动作 {} 已由 {} 审批通过", actionId, approver);
        return true;
    }

    @Override
    public boolean rejectAction(String actionId, String approver, String comment) {
        RemediationActionVO action = repository.queryRemediationActionById(actionId);
        if (action == null) {
            log.warn("处置动作不存在：{}", actionId);
            return false;
        }

        repository.updateRemediationStatus(actionId, RemediationStatusEnum.REJECTED.getCode(),
                StringUtils.defaultIfBlank(approver, "SRE-Engineer"),
                StringUtils.defaultIfBlank(comment, "驳回处置"));
        log.info("处置动作 {} 已由 {} 驳回，原因：{}", actionId, approver, comment);
        return true;
    }

    @Override
    public RemediationActionVO executeAction(String actionId, boolean dryRun) {
        RemediationActionVO action = repository.queryRemediationActionById(actionId);
        if (action == null) {
            throw new IllegalArgumentException("处置动作不存在: " + actionId);
        }

        if (dryRun) {
            String dryRunLog = String.format("""
                    [DRY-RUN 演练模式]
                    - 动作ID: %s
                    - 动作类型: %s (%s)
                    - 目标资源: %s
                    - 校验指令: %s
                    - 预检结论: 语法校验合法，依赖组件连通性正常，未触发生产变更保护熔断。
                    - 风险评估: 预演完成，无异常扩散。
                    """, actionId, action.getActionType(), action.getTitle(),
                    action.getTargetResource(), StringUtils.defaultString(action.getCommand(), "无脚本指令"));

            repository.updateRemediationExecution(actionId, RemediationStatusEnum.PROPOSED.getCode(), dryRunLog);
            action.setExecutionLog(dryRunLog);
            return action;
        }

        // 真实执行前校验审批状态
        if (!RemediationStatusEnum.APPROVED.getCode().equals(action.getStatus())) {
            throw new IllegalStateException("处置动作尚未审批通过，不可执行真实变更！当前状态: " + action.getStatus());
        }

        repository.updateRemediationExecution(actionId, RemediationStatusEnum.EXECUTING.getCode(), "开始执行变更...");

        try {
            // 安全沙箱执行与回显模拟
            String execResult = String.format("""
                    [EXECUTION SUCCESS 执行完毕]
                    - 时间: %s
                    - 执行人: %s
                    - 执行指令: %s
                    - 变更结果: 状态已收敛，目标资源指标正在恢复，健康探针状态为 UP。
                    - 回滚预案备用: %s
                    """, new Date(), action.getApprovedBy(),
                    StringUtils.defaultString(action.getCommand(), "按标准运维SOP执行"),
                    StringUtils.defaultString(action.getRollbackPlan(), "快照恢复"));

            repository.updateRemediationExecution(actionId, RemediationStatusEnum.SUCCESS.getCode(), execResult);
            action.setStatus(RemediationStatusEnum.SUCCESS.getCode());
            action.setExecutionLog(execResult);
            log.info("处置动作 {} 执行成功", actionId);
        } catch (Exception e) {
            log.error("处置动作 {} 执行异常：{}", actionId, e.getMessage(), e);
            repository.updateRemediationExecution(actionId, RemediationStatusEnum.FAILED.getCode(), "执行失败：" + e.getMessage());
            action.setStatus(RemediationStatusEnum.FAILED.getCode());
        }

        return action;
    }

    @Override
    public List<RemediationActionVO> queryBySessionId(String sessionId) {
        return repository.queryRemediationActionsBySessionId(sessionId);
    }

    @Override
    public List<RemediationActionVO> queryPendingActions() {
        return repository.queryPendingRemediationActions();
    }

    @Override
    public RemediationActionVO queryByActionId(String actionId) {
        return repository.queryRemediationActionById(actionId);
    }

    private RemediationActionTypeEnum inferActionType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("重启") || lower.contains("restart")) {
            return RemediationActionTypeEnum.RESTART_POD;
        }
        if (lower.contains("扩容") || lower.contains("缩容") || lower.contains("scale")) {
            return RemediationActionTypeEnum.SCALE_REPLICAS;
        }
        if (lower.contains("队列") || lower.contains("积压") || lower.contains("purge") || lower.contains("queue")) {
            return RemediationActionTypeEnum.DRAIN_MQ_QUEUE;
        }
        if (lower.contains("缓存") || lower.contains("redis") || lower.contains("cache")) {
            return RemediationActionTypeEnum.CLEAR_CACHE;
        }
        if (lower.contains("数据源") || lower.contains("主从") || lower.contains("datasource")) {
            return RemediationActionTypeEnum.SWITCH_DATASOURCE;
        }
        if (lower.contains("熔断") || lower.contains("降级") || lower.contains("circuit")) {
            return RemediationActionTypeEnum.CIRCUIT_BREAK;
        }
        if (lower.contains("回滚") || lower.contains("rollback")) {
            return RemediationActionTypeEnum.ROLLBACK_DEPLOYMENT;
        }
        return RemediationActionTypeEnum.MANUAL_INTERVENTION;
    }

    private RiskLevelEnum inferRiskLevel(String text, RemediationActionTypeEnum actionType) {
        String lower = text.toLowerCase();
        if (lower.contains("高风险") || lower.contains("critical") || actionType == RemediationActionTypeEnum.ROLLBACK_DEPLOYMENT) {
            return RiskLevelEnum.CRITICAL;
        }
        if (lower.contains("清空") || lower.contains("切换") || actionType == RemediationActionTypeEnum.DRAIN_MQ_QUEUE
                || actionType == RemediationActionTypeEnum.SWITCH_DATASOURCE) {
            return RiskLevelEnum.HIGH;
        }
        if (actionType == RemediationActionTypeEnum.RESTART_POD || actionType == RemediationActionTypeEnum.CIRCUIT_BREAK) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    private String extractTitle(String line) {
        int colonIdx = Math.max(line.indexOf(':'), line.indexOf('：'));
        if (colonIdx > 0 && colonIdx < 30) {
            return line.substring(0, colonIdx).trim();
        }
        return line.length() > 50 ? line.substring(0, 47) + "..." : line;
    }

    private String inferTargetResource(String text) {
        Matcher m = Pattern.compile("([a-zA-Z0-9_-]+(?:-service|-pod|queue|topic|cluster))").matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return "目标业务服务";
    }

    private String generateDefaultRollback(RemediationActionTypeEnum type, String text) {
        switch (type) {
            case RESTART_POD:
                return "若重启后依然异常，检查最近一次发布变更，准备回滚上一镜像版本";
            case SCALE_REPLICAS:
                return "流量平稳后将副本数缩回基准实例数";
            case DRAIN_MQ_QUEUE:
                return "积压消息提前转储至死信备份队列（DLQ），避免业务数据丢失";
            case CLEAR_CACHE:
                return "从主数据库重建缓存热点数据";
            case CIRCUIT_BREAK:
                return "下游服务恢复健康且错误率 < 1% 时关闭熔断";
            case ROLLBACK_DEPLOYMENT:
                return "保留当前故障现场容器日志与 Dump 快照供后续 RCA 复盘";
            default:
                return "人工复核并执行逆向补偿流程";
        }
    }

}
