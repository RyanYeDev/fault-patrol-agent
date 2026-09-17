package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientModelVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型配置数据加载策略
 */
@Slf4j
@Service
public class AiClientModelLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<AiClientModelVO> modelList;
        if (AiAgentEnumVO.AI_CLIENT_MODEL.getCode().equals(requestParameter.getCommandType())) {
            // 按模型 ID 装配
            modelList = repository.AiClientModelVOByModelIds(requestParameter.getCommandIdList());
        } else {
            // 按客户端 ID 反查关联模型
            modelList = repository.AiClientModelVOByClientIds(requestParameter.getCommandIdList());
        }
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_MODEL.getDataName(), modelList);
        log.info("装配数据加载完成，模型配置 {} 条", modelList == null ? 0 : modelList.size());
    }

}
