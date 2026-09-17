package cn.faultpatrol.domain.agent.service.task;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentTaskScheduleVO;
import cn.faultpatrol.domain.agent.service.ITaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体执行任务
 *
 * 2025/9/13 16:09
 */
@Service
public class AiAgentTaskService implements ITaskService {

    @Resource
    private IAgentRepository repository;

    @Override
    public List<AiAgentTaskScheduleVO> queryAllValidTaskSchedule() {
        return repository.queryAllValidTaskSchedule();
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        return repository.queryAllInvalidTaskScheduleIds();
    }

}
