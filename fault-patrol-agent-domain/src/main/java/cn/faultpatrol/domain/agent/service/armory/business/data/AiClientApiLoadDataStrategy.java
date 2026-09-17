package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientApiVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * API 配置数据加载策略
 */
@Slf4j
@Service
public class AiClientApiLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<AiClientApiVO> apiList;
        if (AiAgentEnumVO.AI_CLIENT_API.getCode().equals(requestParameter.getCommandType())) {
            // 按 API ID 装配
            apiList = repository.queryAiClientApiVOListByApiIds(requestParameter.getCommandIdList());
        } else {
            // 按客户端 ID 反查关联 API
            apiList = repository.queryAiClientApiVOListByClientIds(requestParameter.getCommandIdList());
        }
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_API.getDataName(), apiList);
        log.info("装配数据加载完成，API 配置 {} 条", apiList == null ? 0 : apiList.size());
    }

}
