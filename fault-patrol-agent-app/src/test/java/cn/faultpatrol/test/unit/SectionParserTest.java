package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.service.execute.diagnose.step.support.SectionParser;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 阶段输出分节解析器单元测试
 */
public class SectionParserTest {

    @Test
    public void testParseStandardSections() {
        String text = "故障分析: 疑似库存异常\n具体分析内容\n"
                + "执行历史评估: 无\n"
                + "取证策略: 查询指标\n"
                + "完成度评估: 30%\n"
                + "任务状态: CONTINUE";

        Map<String, String> sections = SectionParser.parse(text,
                List.of("故障分析", "执行历史评估", "取证策略", "完成度评估", "任务状态"));

        assertEquals(5, sections.size());
        assertEquals("疑似库存异常\n具体分析内容", sections.get("故障分析"));
        assertEquals("无", sections.get("执行历史评估"));
        assertEquals("查询指标", sections.get("取证策略"));
        assertEquals("30%", sections.get("完成度评估"));
        assertEquals("CONTINUE", sections.get("任务状态"));
    }

    @Test
    public void testParseFullWidthColon() {
        String text = "质量评估：整体合格\n问题识别：无";
        Map<String, String> sections = SectionParser.parse(text, List.of("质量评估", "问题识别"));
        assertEquals("整体合格", sections.get("质量评估"));
        assertEquals("无", sections.get("问题识别"));
    }

    @Test
    public void testParseNoMatchReturnsEmpty() {
        String text = "这是一段没有分节标题的输出\n模型自由发挥的内容";
        Map<String, String> sections = SectionParser.parse(text, List.of("故障分析", "取证策略"));
        assertTrue(sections.isEmpty());
    }

    @Test
    public void testParseNullAndBlank() {
        assertTrue(SectionParser.parse(null, List.of("故障分析")).isEmpty());
        assertTrue(SectionParser.parse("  \n ", List.of("故障分析")).isEmpty());
    }

    @Test
    public void testSectionOrderPreserved() {
        String text = "取证结果: B\n取证目标: A";
        Map<String, String> sections = SectionParser.parse(text, List.of("取证目标", "取证结果"));
        assertEquals(List.of("取证结果", "取证目标"), List.copyOf(sections.keySet()));
    }

}
