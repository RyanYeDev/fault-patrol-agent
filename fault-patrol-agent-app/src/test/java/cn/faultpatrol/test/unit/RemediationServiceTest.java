package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.RemediationActionVO;
import cn.faultpatrol.domain.agent.service.remediation.RemediationService;
import cn.faultpatrol.types.enums.RemediationActionTypeEnum;
import cn.faultpatrol.types.enums.RemediationStatusEnum;
import cn.faultpatrol.types.enums.RiskLevelEnum;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 故障处置动作与审批工作流单元测试
 */
public class RemediationServiceTest {

    private RemediationService remediationService;
    private IAgentRepository repository;

    @Before
    public void setUp() {
        remediationService = new RemediationService();
        repository = mock(IAgentRepository.class);
        ReflectionTestUtils.setField(remediationService, "repository", repository);
    }

    @Test
    public void testExtractAndSaveActionsWithMultipleTypes() {
        String remediationMarkdown = """
                根据诊断研判，提出以下处置方案：
                1. 重启订单服务Pod：重启 order-service-pod 释放卡死连接
                ```bash
                kubectl rollout restart deployment/order-service -n prod
                ```
                2. 清空缓存并重建：清理 Redis 热点缓存 key order:cache:hot
                ```bash
                redis-cli -h redis-cluster del order:cache:hot
                ```
                3. 回滚发布版本：若上述操作无效则紧急回滚版本
                """;

        List<RemediationActionVO> actions = remediationService.extractAndSaveActions("sess_1001", remediationMarkdown);

        assertEquals(3, actions.size());

        // Action 1: 重启 Pod -> MEDIUM
        RemediationActionVO act1 = actions.get(0);
        assertEquals(RemediationActionTypeEnum.RESTART_POD.getCode(), act1.getActionType());
        assertEquals(RiskLevelEnum.MEDIUM.getCode(), act1.getRiskLevel());
        assertEquals("sess_1001", act1.getSessionId());
        assertNotNull(act1.getCommand());
        assertTrue(act1.getCommand().contains("kubectl rollout restart"));
        assertTrue(act1.getRollbackPlan().contains("回滚上一镜像版本"));

        // Action 2: 清空缓存 -> HIGH
        RemediationActionVO act2 = actions.get(1);
        assertEquals(RemediationActionTypeEnum.CLEAR_CACHE.getCode(), act2.getActionType());
        assertEquals(RiskLevelEnum.HIGH.getCode(), act2.getRiskLevel());
        assertTrue(act2.getCommand().contains("redis-cli"));

        // Action 3: 回滚 -> CRITICAL
        RemediationActionVO act3 = actions.get(2);
        assertEquals(RemediationActionTypeEnum.ROLLBACK_DEPLOYMENT.getCode(), act3.getActionType());
        assertEquals(RiskLevelEnum.CRITICAL.getCode(), act3.getRiskLevel());

        verify(repository, times(3)).saveRemediationAction(any(RemediationActionVO.class));
    }

    @Test
    public void testExtractAndSaveActionsFallback() {
        String unformattedText = "建议全面复核网络连通性及防火墙端口策略。";
        List<RemediationActionVO> actions = remediationService.extractAndSaveActions("sess_1002", unformattedText);

        assertEquals(1, actions.size());
        RemediationActionVO fallback = actions.get(0);
        assertEquals(RemediationActionTypeEnum.MANUAL_INTERVENTION.getCode(), fallback.getActionType());
        assertEquals(RiskLevelEnum.LOW.getCode(), fallback.getRiskLevel());
        verify(repository, times(1)).saveRemediationAction(any(RemediationActionVO.class));
    }

    @Test
    public void testApproveActionSuccess() {
        RemediationActionVO proposed = RemediationActionVO.builder()
                .actionId("act_001")
                .status(RemediationStatusEnum.PROPOSED.getCode())
                .build();
        when(repository.queryRemediationActionById("act_001")).thenReturn(proposed);

        boolean success = remediationService.approveAction("act_001", "devops-lead", "经确认无误，同意发布重启");
        assertTrue(success);

        verify(repository).updateRemediationStatus(eq("act_001"), eq(RemediationStatusEnum.APPROVED.getCode()),
                eq("devops-lead"), eq("经确认无误，同意发布重启"));
    }

    @Test
    public void testApproveActionAlreadyProcessedReturnsFalse() {
        RemediationActionVO approved = RemediationActionVO.builder()
                .actionId("act_002")
                .status(RemediationStatusEnum.APPROVED.getCode())
                .build();
        when(repository.queryRemediationActionById("act_002")).thenReturn(approved);

        boolean success = remediationService.approveAction("act_002", "devops-lead", "重复审批");
        assertFalse(success);
        verify(repository, never()).updateRemediationStatus(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testRejectActionSuccess() {
        RemediationActionVO proposed = RemediationActionVO.builder()
                .actionId("act_003")
                .status(RemediationStatusEnum.PROPOSED.getCode())
                .build();
        when(repository.queryRemediationActionById("act_003")).thenReturn(proposed);

        boolean success = remediationService.rejectAction("act_003", "sre-lead", "高峰期禁止重启");
        assertTrue(success);
        verify(repository).updateRemediationStatus(eq("act_003"), eq(RemediationStatusEnum.REJECTED.getCode()),
                eq("sre-lead"), eq("高峰期禁止重启"));
    }

    @Test
    public void testExecuteActionDryRun() {
        RemediationActionVO action = RemediationActionVO.builder()
                .actionId("act_004")
                .actionType(RemediationActionTypeEnum.DRAIN_MQ_QUEUE.getCode())
                .title("清空积压死信队列")
                .targetResource("payment-dead-letter-queue")
                .command("rabbitmqadmin purge queue name=payment-dead-letter-queue")
                .status(RemediationStatusEnum.PROPOSED.getCode())
                .build();
        when(repository.queryRemediationActionById("act_004")).thenReturn(action);

        RemediationActionVO result = remediationService.executeAction("act_004", true);
        assertNotNull(result);
        assertNotNull(result.getExecutionLog());
        assertTrue(result.getExecutionLog().contains("DRY-RUN 演练模式"));
        assertTrue(result.getExecutionLog().contains("rabbitmqadmin purge"));
        // Status remains PROPOSED in dry run
        assertEquals(RemediationStatusEnum.PROPOSED.getCode(), result.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testExecuteUnapprovedActionThrowsException() {
        RemediationActionVO unapproved = RemediationActionVO.builder()
                .actionId("act_005")
                .status(RemediationStatusEnum.PROPOSED.getCode())
                .build();
        when(repository.queryRemediationActionById("act_005")).thenReturn(unapproved);

        // 真实执行未审批的动作必须抛出异常
        remediationService.executeAction("act_005", false);
    }

    @Test
    public void testExecuteApprovedActionSuccess() {
        RemediationActionVO approved = RemediationActionVO.builder()
                .actionId("act_006")
                .actionType(RemediationActionTypeEnum.RESTART_POD.getCode())
                .status(RemediationStatusEnum.APPROVED.getCode())
                .approvedBy("sre-expert")
                .command("kubectl rollout restart deployment/cart-service")
                .rollbackPlan("回滚旧版本")
                .build();
        when(repository.queryRemediationActionById("act_006")).thenReturn(approved);

        RemediationActionVO result = remediationService.executeAction("act_006", false);
        assertNotNull(result);
        assertEquals(RemediationStatusEnum.SUCCESS.getCode(), result.getStatus());
        assertTrue(result.getExecutionLog().contains("EXECUTION SUCCESS"));
        verify(repository).updateRemediationExecution(eq("act_006"), eq(RemediationStatusEnum.SUCCESS.getCode()), anyString());
    }

}
