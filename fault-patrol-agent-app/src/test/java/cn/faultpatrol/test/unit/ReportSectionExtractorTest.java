package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.service.execute.diagnose.step.support.ReportSectionExtractor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 诊断报告小节提取器单元测试
 */
public class ReportSectionExtractorTest {

    @Test
    public void testExtractSectionWithSubHeadings() {
        String report = """
                # 诊断报告

                ## 故障概述
                现象描述

                ## 根因分析

                ### 证据链现状
                证据一

                ### 根因研判
                根因结论

                ## 处置建议
                处置步骤
                """;

        String rootCause = ReportSectionExtractor.extractSection(report, "根因分析");
        // ### 及以下标题视为内容，不应截断
        assertTrue(rootCause.contains("### 证据链现状"));
        assertTrue(rootCause.contains("证据一"));
        assertTrue(rootCause.contains("根因结论"));
        // 同级标题后的内容不应包含
        assertTrue(!rootCause.contains("处置步骤"));
    }

    @Test
    public void testExtractSectionStopsAtSiblingHeading() {
        String report = "## 根因分析\n根因内容\n## 处置建议\n处置内容";
        assertEquals("根因内容", ReportSectionExtractor.extractSection(report, "根因分析"));
    }

    @Test
    public void testExtractMissingSectionReturnsEmpty() {
        assertEquals("", ReportSectionExtractor.extractSection("## 根因分析\n内容", "不存在的章节"));
        assertEquals("", ReportSectionExtractor.extractSection(null, "根因分析"));
        assertEquals("", ReportSectionExtractor.extractSection("", "根因分析"));
    }

    @Test
    public void testExtractSkipsBlankLines() {
        String report = "## 处置建议\n\n第一条建议\n\n第二条建议";
        // 提取时忽略空行
        assertEquals("第一条建议\n第二条建议",
                ReportSectionExtractor.extractSection(report, "处置建议"));
    }

}
