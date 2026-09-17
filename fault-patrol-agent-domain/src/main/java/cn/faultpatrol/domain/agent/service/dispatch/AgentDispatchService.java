package cn.faultpatrol.domain.agent.service.dispatch;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentVO;
import cn.faultpatrol.domain.agent.service.IAgentDispatchService;
import cn.faultpatrol.domain.agent.service.IExecuteStrategy;
import cn.faultpatrol.types.exception.BizException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Agent 调度服务
 * <p>
 * 按智能体配置的策略从注册表中获取执行策略 Bean，
 * 在线程池中异步驱动执行，异常以 SSE 错误消息回传并确保流关闭。
 */
@Slf4j
@Service
public class AgentDispatchService implements IAgentDispatchService {

    @Resource
    private Map<String, IExecuteStrategy> executeStrategyMap;

    @Resource
    private IAgentRepository repository;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void dispatch(ExecuteCommandEntity requestParameter, ResponseBodyEmitter emitter) throws Exception {
        AiAgentVO aiAgentVO = repository.queryAiAgentByAgentId(requestParameter.getAiAgentId());

        String strategy = aiAgentVO.getStrategy();
        IExecuteStrategy executeStrategy = executeStrategyMap.get(strategy);
        if (null == executeStrategy) {
            throw new BizException("不存在的执行策略类型 strategy:" + strategy);
        }

        // 异步执行诊断策略
        threadPoolExecutor.execute(() -> {
            try {
                executeStrategy.execute(requestParameter, emitter);
            } catch (Exception e) {
                log.error("诊断执行异常：{}", e.getMessage(), e);
                try {
                    // 以 SSE JSON 结构回传错误信息
                    DiagnoseExecuteResultEntity errorResult = DiagnoseExecuteResultEntity.createErrorResult(
                            "执行异常：" + e.getMessage(), requestParameter.getSessionId());
                    emitter.send("data: " + JSON.toJSONString(errorResult) + "\n\n");
                } catch (Exception ex) {
                    log.error("发送异常信息失败：{}", ex.getMessage(), ex);
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("完成流式输出失败：{}", e.getMessage(), e);
                }
            }
        });

    }

}
