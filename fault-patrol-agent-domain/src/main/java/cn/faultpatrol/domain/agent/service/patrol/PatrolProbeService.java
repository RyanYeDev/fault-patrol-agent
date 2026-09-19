package cn.faultpatrol.domain.agent.service.patrol;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.PatrolTargetVO;
import cn.faultpatrol.domain.agent.service.IAgentDispatchService;
import cn.faultpatrol.types.enums.PatrolProbeTypeEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * 微服务主动巡检探针与异常巡检触发引擎实现
 */
@Slf4j
@Service
public class PatrolProbeService implements IPatrolProbeService {

    @Resource
    private IAgentRepository repository;

    @Resource
    private IAgentDispatchService dispatchService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public ProbeResult probe(PatrolTargetVO target) {
        long start = System.currentTimeMillis();
        PatrolProbeTypeEnum type = PatrolProbeTypeEnum.fromCode(target.getProbeType());

        ProbeResult result;
        try {
            switch (type) {
                case HTTP_HEALTH:
                    result = probeHttp(target, start);
                    break;
                case PROMETHEUS_METRIC:
                    result = probePrometheus(target, start);
                    break;
                case REDIS_HEALTH:
                case RABBITMQ_QUEUE:
                default:
                    result = probeHttp(target, start);
                    break;
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            result = new ProbeResult(false, "UNHEALTHY", "探针执行失败: " + e.getMessage(), latency);
        }

        // 更新数据库中监控目标的最近状态
        repository.updatePatrolTargetCheckStatus(target.getId(), result.status(), result.message());
        return result;
    }

    @Override
    public void runAllActiveProbes() {
        List<PatrolTargetVO> targets = repository.queryActivePatrolTargets();
        if (targets == null || targets.isEmpty()) {
            return;
        }

        log.info("开始执行微服务主动巡检探针任务，监控目标总数: {}", targets.size());
        for (PatrolTargetVO target : targets) {
            try {
                ProbeResult result = probe(target);
                if (!result.healthy()) {
                    handleUnhealthyTarget(target, result);
                } else {
                    log.debug("目标微服务 [{}] 探针健康，耗时: {}ms", target.getServiceName(), result.latencyMs());
                }
            } catch (Exception e) {
                log.error("探针巡检目标 [{}] 发生异常：{}", target.getServiceName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void registerTarget(PatrolTargetVO targetVO) {
        PatrolTargetVO existing = repository.queryPatrolTargetByServiceName(targetVO.getServiceName());
        if (existing == null) {
            repository.savePatrolTarget(targetVO);
            log.info("新注册微服务巡检监控目标：[{}]", targetVO.getServiceName());
        } else {
            log.info("微服务巡检目标 [{}] 已存在，更新配置", targetVO.getServiceName());
        }
    }

    @Override
    public List<PatrolTargetVO> queryAllTargets() {
        return repository.queryAllPatrolTargets();
    }

    private ProbeResult probeHttp(PatrolTargetVO target, long start) {
        String endpoint = target.getTargetEndpoint();
        if (StringUtils.isBlank(endpoint)) {
            return new ProbeResult(false, "UNHEALTHY", "探针未配置有效端点", 0);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                if (body != null && body.contains("\"status\"")) {
                    try {
                        JSONObject obj = JSON.parseObject(body);
                        String st = obj.getString("status");
                        if ("UP".equalsIgnoreCase(st)) {
                            return new ProbeResult(true, "HEALTHY", "服务健康 (UP)", latency);
                        } else {
                            return new ProbeResult(false, "DEGRADED", "服务组件状态降级: " + st, latency);
                        }
                    } catch (Exception ignored) {
                    }
                }
                return new ProbeResult(true, "HEALTHY", "HTTP " + response.statusCode() + " OK", latency);
            } else {
                return new ProbeResult(false, "UNHEALTHY", "HTTP响应状态码异常: " + response.statusCode(), latency);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, "UNHEALTHY", "连接异常: " + e.getMessage(), latency);
        }
    }

    private ProbeResult probePrometheus(PatrolTargetVO target, long start) {
        // Prometheus 表达式检查
        return probeHttp(target, start);
    }

    /**
     * 发现异常服务，检查静默防抖窗口后自主触发诊断 Agent
     */
    private void handleUnhealthyTarget(PatrolTargetVO target, ProbeResult result) {
        log.warn("微服务 [{}] 探针检测异常：{}，状态: {}", target.getServiceName(), result.message(), result.status());

        // 检查防抖与静默窗口（避免故障期间频繁狂轰滥炸拉起智能体）
        Date lastCheck = target.getLastCheckTime();
        int quietMinutes = target.getQuietWindowMinutes() != null ? target.getQuietWindowMinutes() : 30;
        if (lastCheck != null && "UNHEALTHY".equalsIgnoreCase(target.getLastCheckStatus())) {
            long diffMin = (System.currentTimeMillis() - lastCheck.getTime()) / (60 * 1000);
            if (diffMin < quietMinutes) {
                log.info("微服务 [{}] 处于静默防抖窗口期内（已持续异常 {} 分钟 < {} 分钟），抑制本次重复诊断发起",
                        target.getServiceName(), diffMin, quietMinutes);
                return;
            }
        }

        // 自动装配并调度四阶段智能体诊断链路
        String sessionId = "patrol_" + target.getServiceName() + "_" + System.currentTimeMillis();
        String alertMessage = String.format("""
                【主动巡检探针异常告警】
                受影响微服务：%s
                探针端点：%s
                检测状态：%s
                异常描述：%s
                响应耗时：%d ms
                请针对该微服务的异常表现启动自动取证与根因排查，定位问题并给出处置建议。
                """, target.getServiceName(), target.getTargetEndpoint(), result.status(), result.message(), result.latencyMs());

        String agentId = StringUtils.defaultIfBlank(target.getAiAgentId(), "10001");
        ExecuteCommandEntity command = ExecuteCommandEntity.builder()
                .aiAgentId(agentId)
                .message(alertMessage)
                .sessionId(sessionId)
                .maxStep(3)
                .build();

        log.info("主动巡检触发自主诊断 Agent，sessionId: {}, service: {}", sessionId, target.getServiceName());
        try {
            dispatchService.dispatch(command, new ResponseBodyEmitter());
        } catch (Exception e) {
            log.error("主动巡检触发诊断 Agent 失败：{}", e.getMessage(), e);
        }
    }

}
