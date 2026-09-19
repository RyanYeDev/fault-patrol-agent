package cn.faultpatrol.domain.agent.service.learning;

import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.service.IRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 故障诊断经验自主学习与 Postmortem 知识库沉淀实现
 */
@Slf4j
@Service
public class PostmortemService implements IPostmortemService {

    @Resource
    private IRagService ragService;

    @Override
    public String learnAndSynthesizePlaybook(DiagnosisReportVO reportVO) {
        if (reportVO == null) {
            return null;
        }

        // 仅对明确诊断完成、且具有根因结论的报告进行知识库提炼
        if (!"COMPLETED".equalsIgnoreCase(reportVO.getStatus()) || StringUtils.isBlank(reportVO.getRootCause())) {
            log.info("报告未达到收敛完成标准，跳过经验沉淀：sessionId={}", reportVO.getSessionId());
            return null;
        }

        String title = extractShortTitle(reportVO.getAlertContent());
        String playbookMarkdown = String.format("""
                # 故障实战排查手册: %s
                - **归档会话**: `%s`
                - **生成时间**: %s
                - **排查模式**: Fault Patrol SRE Autonomous Plan-and-Execute

                ## 1. 故障现场与告警现象
                > %s

                ## 2. 定性结论与根因剖析
                %s

                ## 3. 经过实战验证的取证特征与排查路径
                %s

                ## 4. 推荐处置方案与应急预案
                %s

                ## 5. 预防与治理建议
                - 建立同类指标基线告警，预防故障突发扩散；
                - 本手册由 Fault Patrol 自主进化学习系统自动归档，供后续相似故障比对召回。
                """,
                title,
                reportVO.getSessionId(),
                new Date(),
                StringUtils.defaultIfBlank(reportVO.getAlertContent(), "未记录告警内容"),
                reportVO.getRootCause(),
                StringUtils.defaultIfBlank(reportVO.getEvidence(), "指标、链路与数据交叉验证"),
                StringUtils.defaultIfBlank(reportVO.getRemediation(), "按运维SOP执行"));

        try {
            // 写入 RAG 向量知识库（标签统一为 fault-handbook，供后续诊断自动检索）
            String docName = "实战复盘-" + title;
            ragService.storeTextContent(docName, "fault-handbook", playbookMarkdown, "postmortem-" + reportVO.getSessionId() + ".md");
            log.info("已将实战故障手册自动沉淀入库：title={}, sessionId={}", docName, reportVO.getSessionId());
            return playbookMarkdown;
        } catch (Exception e) {
            log.error("将故障手册沉淀至知识库失败（不影响主流程）：{}", e.getMessage(), e);
            return null;
        }
    }

    private String extractShortTitle(String alertContent) {
        if (StringUtils.isBlank(alertContent)) {
            return "线上故障诊断实战";
        }
        String firstLine = alertContent.split("\n")[0].trim();
        firstLine = firstLine.replaceAll("^[#*【\\[]+\\s*", "").replaceAll("[】\\]]+", "");
        return firstLine.length() > 30 ? firstLine.substring(0, 27) + "..." : firstLine;
    }

}
