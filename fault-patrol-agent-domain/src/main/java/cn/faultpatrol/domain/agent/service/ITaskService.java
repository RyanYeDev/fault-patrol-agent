package cn.faultpatrol.domain.agent.service;

import cn.faultpatrol.domain.agent.model.valobj.AiAgentTaskScheduleVO;

import java.util.List;

/**
 * 智能体执行任务
 * 2025/9/13 16:08
 */
public interface ITaskService {

    List<AiAgentTaskScheduleVO> queryAllValidTaskSchedule();

    List<Long> queryAllInvalidTaskScheduleIds();

}
