package cn.faultpatrol.domain.agent.service.execute.diagnose;

import cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.service.IExecuteStrategy;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.factory.DefaultDiagnoseAgentExecuteStrategyFactory;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * 巡检诊断执行策略（Plan-and-Execute 四阶段）
 * <p>
 * 规划(Plan) → 取证执行(Evidence) → 质量监督(Supervision) → 诊断报告(Report)，
 * 通过责任链驱动阶段流转：质量监督不通过时回环到规划节点重新取证，
 * 直至通过或达到最大步数；全链路 SSE 流式输出各阶段过程。
 */
@Slf4j
@Service("diagnoseAgentExecuteStrategy")
public class DiagnoseAgentExecuteStrategy implements IExecuteStrategy {

    @Resource
    private DefaultDiagnoseAgentExecuteStrategyFactory diagnoseAgentExecuteStrategyFactory;

    @Override
    public void execute(ExecuteCommandEntity executeCommandEntity, ResponseBodyEmitter emitter) throws Exception {
        StrategyHandler<ExecuteCommandEntity, DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext, String> executeHandler
                = diagnoseAgentExecuteStrategyFactory.armoryStrategyHandler();

        // 创建动态上下文并初始化必要字段
        DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext dynamicContext = new DefaultDiagnoseAgentExecuteStrategyFactory.DynamicContext();
        dynamicContext.setMaxStep(executeCommandEntity.getMaxStep() != null ? executeCommandEntity.getMaxStep() : 3);
        dynamicContext.setExecutionHistory(new StringBuilder());
        dynamicContext.setCurrentTask(executeCommandEntity.getMessage());
        dynamicContext.setValue("emitter", emitter);

        String apply = executeHandler.apply(executeCommandEntity, dynamicContext);
        log.info("诊断执行链完成: {}", apply);

        // 发送完成标识
        try {
            DiagnoseExecuteResultEntity completeResult = DiagnoseExecuteResultEntity.createCompleteResult(executeCommandEntity.getSessionId());
            // 发送SSE格式的数据
            String sseData = "data: " + JSON.toJSONString(completeResult) + "\n\n";
            emitter.send(sseData);
        } catch (Exception e) {
            log.error("发送完成标识失败：{}", e.getMessage(), e);
        }
    }

}
