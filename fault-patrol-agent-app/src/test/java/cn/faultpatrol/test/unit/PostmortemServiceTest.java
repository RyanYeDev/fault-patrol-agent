package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.service.IRagService;
import cn.faultpatrol.domain.agent.service.learning.PostmortemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 诊断经验自主学习与知识库沉淀 (Hermes Self-Learning Postmortem) 单元测试
 */
public class PostmortemServiceTest {

    private PostmortemService postmortemService;
    private IRagService ragService;

    @Before
    public void setUp() {
        postmortemService = new PostmortemService();
        ragService = mock(IRagService.class);
        ReflectionTestUtils.setField(postmortemService, "ragService", ragService);
    }

    @Test
    public void testIncompleteReportDoesNotTriggerLearning() {
        DiagnosisReportVO reportVO = DiagnosisReportVO.builder()
                .sessionId("sess_incomplete")
                .status("RUNNING")
                .alertContent("CPU 告警")
                .build();

        String result = postmortemService.learnAndSynthesizePlaybook(reportVO);
        assertNull(result);
        verify(ragService, never()).storeTextContent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testEmptyRootCauseDoesNotTriggerLearning() {
        DiagnosisReportVO reportVO = DiagnosisReportVO.builder()
                .sessionId("sess_no_root_cause")
                .status("COMPLETED")
                .alertContent("内存告警")
                .rootCause("")
                .build();

        String result = postmortemService.learnAndSynthesizePlaybook(reportVO);
        assertNull(result);
        verify(ragService, never()).storeTextContent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testCompletedReportSynthesizesPlaybookAndIndexesToRag() {
        DiagnosisReportVO reportVO = DiagnosisReportVO.builder()
                .sessionId("sess_success_001")
                .status("COMPLETED")
                .alertContent("【Prometheus Alertmanager】\n受影响服务: order-service\n告警摘要: JVM Old Gen Full GC")
                .rootCause("Druid 数据库连接池 leak，由于未配置 testWhileIdle 导致死连接占满物理连接")
                .evidence("GC 日志显示每分钟发生 4 次 Full GC，Dump 分析 DruidConnectionEntry 占比 78%")
                .remediation("重启 order-service Pod 临时恢复，修改 application.yml 配置 testWhileIdle=true 并增加超时回收")
                .build();

        String playbook = postmortemService.learnAndSynthesizePlaybook(reportVO);

        assertNotNull(playbook);
        assertTrue(playbook.contains("# 故障实战排查手册"));
        assertTrue(playbook.contains("sess_success_001"));
        assertTrue(playbook.contains("Druid 数据库连接池 leak"));
        assertTrue(playbook.contains("DruidConnectionEntry 占比 78%"));
        assertTrue(playbook.contains("testWhileIdle=true"));
        assertTrue(playbook.contains("Fault Patrol 自主进化学习系统"));

        // 验证自动归档到 fault-handbook 标签
        verify(ragService).storeTextContent(
                contains("实战复盘"),
                eq("fault-handbook"),
                eq(playbook),
                eq("postmortem-sess_success_001.md")
        );
    }

}
