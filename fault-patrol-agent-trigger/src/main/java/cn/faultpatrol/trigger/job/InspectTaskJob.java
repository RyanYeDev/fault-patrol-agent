package cn.faultpatrol.trigger.job;

import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentTaskScheduleVO;
import cn.faultpatrol.domain.agent.service.IAgentDispatchService;
import cn.faultpatrol.domain.agent.service.ITaskService;
import cn.faultpatrol.types.job.model.TaskScheduleVO;
import cn.faultpatrol.types.job.provider.ITaskDataProvider;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时巡检任务提供者
 * <p>
 * 读取 ai_agent_task_schedule 表中的有效巡检计划，
 * 按 cron 表达式驱动诊断执行链完成定时巡检，
 * taskParam 可携带巡检指令：{"message": "..."}。
 */
@Slf4j
@Service
public class InspectTaskJob implements ITaskDataProvider {

    @Resource
    private ITaskService taskService;

    @Resource
    private IAgentDispatchService dispatchService;

    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<AiAgentTaskScheduleVO> aiAgentTaskScheduleVOS = taskService.queryAllValidTaskSchedule();
        List<TaskScheduleVO> result = new ArrayList<>();
        for (AiAgentTaskScheduleVO aiAgentTaskScheduleVO : aiAgentTaskScheduleVOS) {
            TaskScheduleVO taskScheduleVO = new TaskScheduleVO();
            taskScheduleVO.setId(aiAgentTaskScheduleVO.getId());
            taskScheduleVO.setDescription(aiAgentTaskScheduleVO.getDescription());
            taskScheduleVO.setCronExpression(aiAgentTaskScheduleVO.getCronExpression());
            taskScheduleVO.setTaskParam(aiAgentTaskScheduleVO.getTaskParam());
            taskScheduleVO.setTaskLogic(() -> {
                try {
                    dispatchService.dispatch(
                            ExecuteCommandEntity.builder()
                                    .aiAgentId(aiAgentTaskScheduleVO.getAgentId())
                                    .message(parseTaskMessage(aiAgentTaskScheduleVO.getTaskParam()))
                                    .sessionId("schedule_" + System.nanoTime())
                                    .maxStep(3)
                                    .build(), new ResponseBodyEmitter());
                } catch (Exception e) {
                    log.error("定时巡检任务执行失败", e);
                }
            });

            result.add(taskScheduleVO);
        }
        return result;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        return taskService.queryAllInvalidTaskScheduleIds();
    }

    /**
     * 解析巡检指令：taskParam 为 JSON 时取 message 字段，否则使用默认巡检指令
     */
    private String parseTaskMessage(String taskParam) {
        if (StringUtils.isBlank(taskParam)) {
            return "执行定时巡检任务，检查系统各项指标是否存在异常";
        }
        try {
            JSONObject jsonObject = JSON.parseObject(taskParam);
            String message = jsonObject.getString("message");
            return StringUtils.isBlank(message) ? taskParam : message;
        } catch (Exception e) {
            return taskParam;
        }
    }

}
