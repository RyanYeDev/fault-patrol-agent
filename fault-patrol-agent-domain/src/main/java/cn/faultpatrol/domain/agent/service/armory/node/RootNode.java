package cn.faultpatrol.domain.agent.service.armory.node;

import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.service.armory.business.data.ILoadDataStrategy;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 装配根节点：按命令类型选择数据加载策略，将组件配置载入上下文
 */
@Slf4j
@Service
public class RootNode extends AbstractArmorySupport {

    private final Map<String, ILoadDataStrategy> loadDataStrategyMap;

    @Resource
    private AiClientApiNode aiClientApiNode;

    public RootNode(Map<String, ILoadDataStrategy> loadDataStrategyMap) {
        this.loadDataStrategyMap = loadDataStrategyMap;
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 按命令类型加载配置数据
        ILoadDataStrategy loadDataStrategy = loadDataStrategyMap.get(requestParameter.getLoadDataStrategy());
        loadDataStrategy.loadData(requestParameter, dynamicContext);

        log.info("Agent 装配数据加载节点{}", JSON.toJSONString(requestParameter));
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientApiNode;
    }

}
