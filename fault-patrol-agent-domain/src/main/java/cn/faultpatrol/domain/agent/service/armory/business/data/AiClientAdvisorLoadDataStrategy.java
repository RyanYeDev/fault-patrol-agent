package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientAdvisorVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Advisor 顾问配置数据加载策略
 */
@Slf4j
@Service
public class AiClientAdvisorLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<AiClientAdvisorVO> advisorList = repository.AiClientAdvisorVOByClientIds(requestParameter.getCommandIdList());
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_ADVISOR.getDataName(), advisorList);
        log.info("装配数据加载完成，Advisor 配置 {} 条", advisorList == null ? 0 : advisorList.size());
    }

}
