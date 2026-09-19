package cn.faultpatrol.trigger.job;

import cn.faultpatrol.domain.agent.model.valobj.PatrolTargetVO;
import cn.faultpatrol.domain.agent.service.patrol.IPatrolProbeService;
import cn.faultpatrol.types.job.model.TaskScheduleVO;
import cn.faultpatrol.types.job.provider.ITaskDataProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 微服务主动巡检 Watchdog 任务提供者
 * <p>
 * 周期性执行监控目标健康探针，若检测到指标恶化或服务宕机，
 * 自动联动 Plan-and-Execute 诊断智能体发起交叉取证排查。
 */
@Slf4j
@Service
public class WatchdogTaskJob implements ITaskDataProvider {

    @Resource
    private IPatrolProbeService patrolProbeService;

    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<PatrolTargetVO> targets = patrolProbeService.queryAllTargets();
        List<TaskScheduleVO> list = new ArrayList<>();
        if (targets == null) return list;

        for (PatrolTargetVO target : targets) {
            if (target.getStatus() == null || target.getStatus() != 1) {
                continue;
            }

            String cron = StringUtils.isNotBlank(target.getIntervalCron()) ? target.getIntervalCron() : "0 0/10 * * * ?";
            TaskScheduleVO taskScheduleVO = new TaskScheduleVO();
            // 采用 1000000 偏移量避免与原有 ai_agent_task_schedule ID 冲突
            taskScheduleVO.setId(1000000L + target.getId());
            taskScheduleVO.setDescription("Watchdog主动巡检探针: " + target.getServiceName());
            taskScheduleVO.setCronExpression(cron);
            taskScheduleVO.setTaskLogic(() -> {
                try {
                    IPatrolProbeService.ProbeResult res = patrolProbeService.probe(target);
                    if (!res.healthy()) {
                        log.warn("Watchdog 探测到服务 [{}] 异常: {}", target.getServiceName(), res.message());
                    }
                } catch (Exception e) {
                    log.error("Watchdog 执行探针巡检异常 [{}]: {}", target.getServiceName(), e.getMessage(), e);
                }
            });

            list.add(taskScheduleVO);
        }

        return list;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        return List.of();
    }

}
