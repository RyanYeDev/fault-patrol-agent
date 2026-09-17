package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientSystemPromptVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 系统提示词配置数据加载策略
 */
@Slf4j
@Service
public class AiClientSystemPromptLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        Map<String, AiClientSystemPromptVO> promptMap = repository.queryAiClientSystemPromptMapByClientIds(requestParameter.getCommandIdList());
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_SYSTEM_PROMPT.getDataName(), promptMap);
        log.info("装配数据加载完成，提示词配置 {} 条", promptMap == null ? 0 : promptMap.size());
    }

}
