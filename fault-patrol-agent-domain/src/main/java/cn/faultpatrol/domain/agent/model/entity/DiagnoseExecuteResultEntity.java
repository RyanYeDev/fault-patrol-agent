package cn.faultpatrol.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 诊断执行结果实体（SSE 流式输出载荷）
 * <p>
 * 消息类型 type：plan(故障分析规划), evidence(取证执行), supervision(质量监督), report(诊断报告), error(错误信息), complete(完成标识)
 * 细分类型 subType：
 * plan_status(故障分析), plan_history(历史评估), plan_strategy(取证策略), plan_progress(完成度评估)
 * evidence_target(取证目标), evidence_process(取证过程), evidence_result(取证结果), evidence_quality(证据检查)
 * assessment(质量评估), issues(问题识别), suggestions(改进建议), score(质量评分), pass(检查结果)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiagnoseExecuteResultEntity {

    /**
     * 数据类型：plan(规划阶段), evidence(取证执行阶段), supervision(监督阶段), report(报告阶段), error(错误信息), complete(完成标识)
     */
    private String type;

    /**
     * 子类型标识，用于前端细粒度展示
     */
    private String subType;

    /**
     * 流式增量标记：true 表示该条 SSE 是报告阶段的流式 token 增量，前端应追加到最近一个气泡而不是新建气泡
     */
    private Boolean delta;

    /**
     * 当前步骤
     */
    private Integer step;

    /**
     * 数据内容
     */
    private String content;

    /**
     * 是否完成
     */
    private Boolean completed;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建规划阶段结果
     */
    public static DiagnoseExecuteResultEntity createPlanResult(Integer step, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("plan")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建规划阶段细分结果
     */
    public static DiagnoseExecuteResultEntity createPlanSubResult(Integer step, String subType, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("plan")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建取证执行阶段结果
     */
    public static DiagnoseExecuteResultEntity createEvidenceResult(Integer step, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("evidence")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建取证执行阶段细分结果
     */
    public static DiagnoseExecuteResultEntity createEvidenceSubResult(Integer step, String subType, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("evidence")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建监督阶段结果
     */
    public static DiagnoseExecuteResultEntity createSupervisionResult(Integer step, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("supervision")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建监督阶段细分结果
     */
    public static DiagnoseExecuteResultEntity createSupervisionSubResult(Integer step, String subType, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("supervision")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建报告阶段细分的结果
     */
    public static DiagnoseExecuteResultEntity createReportSubResult(String subType, String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("report")
                .subType(subType)
                .step(4)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建报告阶段结果
     */
    public static DiagnoseExecuteResultEntity createReportResult(String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("report")
                .step(null)
                .content(content)
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建错误结果
     */
    public static DiagnoseExecuteResultEntity createErrorResult(String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("error")
                .step(null)
                .content(content)
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建报告阶段流式 token 增量结果
     */
    public static DiagnoseExecuteResultEntity createReportStreamResult(String content, String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("report")
                .step(null)
                .content(content)
                .delta(true)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建完成标识
     */
    public static DiagnoseExecuteResultEntity createCompleteResult(String sessionId) {
        return DiagnoseExecuteResultEntity.builder()
                .type("complete")
                .step(null)
                .content("执行完成")
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

}
