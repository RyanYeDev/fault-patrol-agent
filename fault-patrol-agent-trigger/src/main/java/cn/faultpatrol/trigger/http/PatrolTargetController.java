package cn.faultpatrol.trigger.http;

import cn.faultpatrol.api.dto.MicroserviceHeartbeatDTO;
import cn.faultpatrol.api.dto.PatrolTargetDTO;
import cn.faultpatrol.api.response.Response;
import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.PatrolTargetVO;
import cn.faultpatrol.domain.agent.service.patrol.IPatrolProbeService;
import cn.faultpatrol.types.enums.ResponseCode;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 巡检监控目标与微服务接入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inspect")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class PatrolTargetController {

    @Resource
    private IPatrolProbeService patrolProbeService;

    @Resource
    private IAgentRepository repository;

    /**
     * 查询所有巡检监控目标
     */
    @RequestMapping(value = "patrol/targets", method = RequestMethod.GET)
    public Response<List<PatrolTargetDTO>> listTargets() {
        try {
            List<PatrolTargetVO> targets = patrolProbeService.queryAllTargets();
            List<PatrolTargetDTO> dtos = targets.stream().map(this::toDTO).collect(Collectors.toList());
            return Response.<List<PatrolTargetDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("查询巡检监控目标列表异常：{}", e.getMessage(), e);
            return Response.<List<PatrolTargetDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 添加/更新监控目标
     */
    @RequestMapping(value = "patrol/target", method = RequestMethod.POST)
    public Response<Boolean> saveTarget(@RequestBody PatrolTargetDTO request) {
        try {
            if (request == null || request.getServiceName() == null || request.getTargetEndpoint() == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("服务名称与探针端点不能为空")
                        .data(false)
                        .build();
            }

            PatrolTargetVO vo = PatrolTargetVO.builder()
                    .serviceName(request.getServiceName())
                    .probeType(request.getProbeType() != null ? request.getProbeType() : "HTTP_HEALTH")
                    .targetEndpoint(request.getTargetEndpoint())
                    .thresholdConfig(request.getThresholdConfig() != null ? request.getThresholdConfig() : "{\"expectedStatus\":\"UP\"}")
                    .intervalCron(request.getIntervalCron() != null ? request.getIntervalCron() : "0 0/10 * * * ?")
                    .aiAgentId(request.getAiAgentId() != null ? request.getAiAgentId() : "10001")
                    .quietWindowMinutes(request.getQuietWindowMinutes() != null ? request.getQuietWindowMinutes() : 30)
                    .status(request.getStatus() != null ? request.getStatus() : 1)
                    .build();

            patrolProbeService.registerTarget(vo);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("注册监控目标成功")
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("保存监控目标异常：{}", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("保存失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    /**
     * 触发对指定监控目标的即时探针检测
     */
    @RequestMapping(value = "patrol/target/probe-now", method = RequestMethod.POST)
    public Response<IPatrolProbeService.ProbeResult> probeNow(@RequestParam String serviceName) {
        try {
            PatrolTargetVO target = repository.queryPatrolTargetByServiceName(serviceName);
            if (target == null) {
                return Response.<IPatrolProbeService.ProbeResult>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("监控目标不存在: " + serviceName)
                        .build();
            }

            IPatrolProbeService.ProbeResult result = patrolProbeService.probe(target);
            return Response.<IPatrolProbeService.ProbeResult>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("探针执行成功")
                    .data(result)
                    .build();
        } catch (Exception e) {
            log.error("即时探针探测异常：{}", e.getMessage(), e);
            return Response.<IPatrolProbeService.ProbeResult>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("探测异常：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 外部微服务自主心跳与自动注册端点
     */
    @RequestMapping(value = "microservice/register", method = RequestMethod.POST)
    public Response<Boolean> registerMicroservice(@RequestBody MicroserviceHeartbeatDTO heartbeat) {
        try {
            if (heartbeat == null || heartbeat.getServiceName() == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("serviceName不能为空")
                        .data(false)
                        .build();
            }

            String baseUrl = heartbeat.getBaseUrl();
            if (baseUrl == null && heartbeat.getInstanceId() != null && heartbeat.getInstanceId().startsWith("http")) {
                baseUrl = heartbeat.getInstanceId();
            }
            if (baseUrl == null) {
                baseUrl = "http://" + heartbeat.getServiceName();
            }

            String healthPath = heartbeat.getHealthPath() != null ? heartbeat.getHealthPath() : "/actuator/health";
            String fullEndpoint = baseUrl.replaceAll("/+$", "") + healthPath;

            PatrolTargetVO targetVO = PatrolTargetVO.builder()
                    .serviceName(heartbeat.getServiceName())
                    .probeType("HTTP_HEALTH")
                    .targetEndpoint(fullEndpoint)
                    .thresholdConfig("{\"expectedStatus\":\"UP\"}")
                    .intervalCron("0 0/10 * * * ?")
                    .aiAgentId("10001")
                    .quietWindowMinutes(30)
                    .status(1)
                    .build();

            patrolProbeService.registerTarget(targetVO);
            log.info("微服务 [{}] 自主上报注册成功，健康端点: {}", heartbeat.getServiceName(), fullEndpoint);

            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("微服务注册成功，已纳入主动巡检监控体系")
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("微服务自主注册异常：{}", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("注册异常：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    private PatrolTargetDTO toDTO(PatrolTargetVO vo) {
        if (vo == null) return null;
        return PatrolTargetDTO.builder()
                .id(vo.getId())
                .serviceName(vo.getServiceName())
                .probeType(vo.getProbeType())
                .targetEndpoint(vo.getTargetEndpoint())
                .thresholdConfig(vo.getThresholdConfig())
                .intervalCron(vo.getIntervalCron())
                .aiAgentId(vo.getAiAgentId())
                .quietWindowMinutes(vo.getQuietWindowMinutes())
                .status(vo.getStatus())
                .lastCheckTime(vo.getLastCheckTime())
                .lastCheckStatus(vo.getLastCheckStatus())
                .lastErrorMsg(vo.getLastErrorMsg())
                .createTime(vo.getCreateTime())
                .build();
    }

}
