package cn.faultpatrol.test.unit;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.PatrolTargetVO;
import cn.faultpatrol.domain.agent.service.IAgentDispatchService;
import cn.faultpatrol.domain.agent.service.patrol.IPatrolProbeService;
import cn.faultpatrol.domain.agent.service.patrol.PatrolProbeService;
import cn.faultpatrol.types.enums.PatrolProbeTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 主动巡检探针与静默防抖引擎单元测试
 */
public class PatrolProbeServiceTest {

    private PatrolProbeService probeService;
    private IAgentRepository repository;
    private IAgentDispatchService dispatchService;

    @Before
    public void setUp() {
        probeService = new PatrolProbeService();
        repository = mock(IAgentRepository.class);
        dispatchService = mock(IAgentDispatchService.class);

        ReflectionTestUtils.setField(probeService, "repository", repository);
        ReflectionTestUtils.setField(probeService, "dispatchService", dispatchService);
    }

    @Test
    public void testRegisterTargetNew() {
        PatrolTargetVO target = PatrolTargetVO.builder()
                .serviceName("order-service")
                .probeType(PatrolProbeTypeEnum.HTTP_HEALTH.getCode())
                .targetEndpoint("http://localhost:8080/actuator/health")
                .status(1)
                .build();

        when(repository.queryPatrolTargetByServiceName("order-service")).thenReturn(null);

        probeService.registerTarget(target);
        verify(repository).savePatrolTarget(target);
    }

    @Test
    public void testRegisterTargetAlreadyExists() {
        PatrolTargetVO existing = PatrolTargetVO.builder()
                .serviceName("order-service")
                .build();
        when(repository.queryPatrolTargetByServiceName("order-service")).thenReturn(existing);

        probeService.registerTarget(existing);
        verify(repository, never()).savePatrolTarget(existing);
    }

    @Test
    public void testQuietWindowSuppressionSuppressesDispatch() throws Exception {
        // 最近一次巡检在 5 分钟前（静默窗口为 30 分钟），且状态为 UNHEALTHY
        Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
        PatrolTargetVO target = PatrolTargetVO.builder()
                .serviceName("payment-service")
                .targetEndpoint("http://localhost:8081/actuator/health")
                .probeType(PatrolProbeTypeEnum.HTTP_HEALTH.getCode())
                .lastCheckTime(fiveMinutesAgo)
                .lastCheckStatus("UNHEALTHY")
                .quietWindowMinutes(30)
                .aiAgentId("10001")
                .build();

        IPatrolProbeService.ProbeResult failResult = new IPatrolProbeService.ProbeResult(
                false, "UNHEALTHY", "Connection timed out", 3000
        );

        ReflectionTestUtils.invokeMethod(probeService, "handleUnhealthyTarget", target, failResult);

        // 验证由于处于 30 分钟防抖静默期内，抑制发起诊断 Agent
        verify(dispatchService, never()).dispatch(any(ExecuteCommandEntity.class), any(ResponseBodyEmitter.class));
    }

    @Test
    public void testQuietWindowExpiredTriggersDispatch() throws Exception {
        // 最近一次巡检在 45 分钟前（已超出 30 分钟静默窗口）
        Date fortyFiveMinutesAgo = new Date(System.currentTimeMillis() - 45 * 60 * 1000);
        PatrolTargetVO target = PatrolTargetVO.builder()
                .serviceName("inventory-service")
                .targetEndpoint("http://localhost:8082/actuator/health")
                .probeType(PatrolProbeTypeEnum.HTTP_HEALTH.getCode())
                .lastCheckTime(fortyFiveMinutesAgo)
                .lastCheckStatus("UNHEALTHY")
                .quietWindowMinutes(30)
                .aiAgentId("10001")
                .build();

        IPatrolProbeService.ProbeResult failResult = new IPatrolProbeService.ProbeResult(
                false, "UNHEALTHY", "HTTP 500 Internal Server Error", 120
        );

        ReflectionTestUtils.invokeMethod(probeService, "handleUnhealthyTarget", target, failResult);

        // 验证静默期已过，成功触发自主诊断 Agent
        verify(dispatchService, times(1)).dispatch(any(ExecuteCommandEntity.class), any(ResponseBodyEmitter.class));
    }

    @Test
    public void testFirstTimeFailureTriggersDispatch() throws Exception {
        // 首次巡检失败（lastCheckTime 为 null）
        PatrolTargetVO target = PatrolTargetVO.builder()
                .serviceName("user-service")
                .targetEndpoint("http://localhost:8083/actuator/health")
                .probeType(PatrolProbeTypeEnum.HTTP_HEALTH.getCode())
                .lastCheckTime(null)
                .lastCheckStatus(null)
                .quietWindowMinutes(30)
                .aiAgentId("10001")
                .build();

        IPatrolProbeService.ProbeResult failResult = new IPatrolProbeService.ProbeResult(
                false, "UNHEALTHY", "Connection refused", 50
        );

        ReflectionTestUtils.invokeMethod(probeService, "handleUnhealthyTarget", target, failResult);

        // 首次故障立即拉起诊断链路
        verify(dispatchService, times(1)).dispatch(any(ExecuteCommandEntity.class), any(ResponseBodyEmitter.class));
    }

}
