package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.model.valobj.NotificationChannelVO;
import cn.faultpatrol.domain.agent.service.notify.NotificationService;
import cn.faultpatrol.types.enums.NotificationChannelTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 故障告警与诊断报告通知中心单元测试
 */
public class NotificationServiceTest {

    private NotificationService notificationService;
    private IAgentRepository repository;

    @Before
    public void setUp() {
        notificationService = new NotificationService();
        repository = mock(IAgentRepository.class);
        ReflectionTestUtils.setField(notificationService, "repository", repository);
    }

    @Test
    public void testSendReportNotificationWithNoChannels() {
        when(repository.queryActiveNotificationChannels()).thenReturn(List.of());

        DiagnosisReportVO report = DiagnosisReportVO.builder()
                .sessionId("sess_123")
                .status("COMPLETED")
                .alertContent("CPU告警")
                .build();

        // 没配置渠道时不抛异常
        notificationService.sendDiagnosisReportNotification(report);
        verify(repository).queryActiveNotificationChannels();
    }

    @Test
    public void testHmacSha256Signature() throws Exception {
        String stringToSign = "1678900000000\nSEC_TEST_SECRET";
        String secret = "SEC_TEST_SECRET";

        String signature = ReflectionTestUtils.invokeMethod(
                notificationService, "signHmacSha256", stringToSign, secret
        );

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        // Base64 HMAC-SHA256 结果长度通常为 44 个字符 (32 字节 Base64 编码)
        assertEquals(44, signature.length());
    }

    @Test
    public void testBuildReportMarkdown() {
        DiagnosisReportVO report = DiagnosisReportVO.builder()
                .sessionId("sess_md_001")
                .status("COMPLETED")
                .createTime(new Date())
                .alertContent("【微服务健康检查异常】")
                .rootCause("连接池打满")
                .remediation("扩容副本")
                .build();

        String markdown = ReflectionTestUtils.invokeMethod(
                notificationService, "buildReportMarkdown", report
        );

        assertNotNull(markdown);
        assertTrue(markdown.contains("### 🚨 【Fault Patrol 故障巡检诊断报告】"));
        assertTrue(markdown.contains("sess_md_001"));
        assertTrue(markdown.contains("连接池打满"));
        assertTrue(markdown.contains("扩容副本"));
    }

    @Test
    public void testSaveAndQueryChannels() {
        NotificationChannelVO channel = NotificationChannelVO.builder()
                .channelId("ch_feishu")
                .channelName("SRE飞书告警群")
                .channelType(NotificationChannelTypeEnum.FEISHU.getCode())
                .webhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/xxx")
                .status(1)
                .build();

        notificationService.saveChannel(channel);
        verify(repository).saveNotificationChannel(channel);

        when(repository.queryActiveNotificationChannels()).thenReturn(List.of(channel));
        List<NotificationChannelVO> activeList = notificationService.queryActiveChannels();
        assertEquals(1, activeList.size());
        assertEquals("ch_feishu", activeList.get(0).getChannelId());
    }

}
