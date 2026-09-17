package cn.faultpatrol.domain.agent.service.execute.diagnose.step.factory;

import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.faultpatrol.domain.agent.service.execute.diagnose.step.RootNode;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 诊断执行策略工厂：注入责任链根节点，产出执行链入口
 */
@Service
public class DefaultDiagnoseAgentExecuteStrategyFactory {

    private final RootNode executeRootNode;

    public DefaultDiagnoseAgentExecuteStrategyFactory(RootNode executeRootNode) {
        this.executeRootNode = executeRootNode;
    }

    public StrategyHandler<ExecuteCommandEntity, DynamicContext, String> armoryStrategyHandler() {
        return executeRootNode;
    }

    /**
     * 诊断动态上下文：贯穿四阶段链路，承载步骤、历史与阶段产物
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        // 任务执行步骤
        private int step = 1;

        // 最大任务步骤
        private int maxStep = 1;

        // 执行历史
        private StringBuilder executionHistory;

        // 当前任务
        private String currentTask;

        // 是否已完成
        boolean isCompleted = false;

        // 智能体流程配置（clientType -> 流程配置）
        private Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap;

        // 通用数据对象
        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }

}
