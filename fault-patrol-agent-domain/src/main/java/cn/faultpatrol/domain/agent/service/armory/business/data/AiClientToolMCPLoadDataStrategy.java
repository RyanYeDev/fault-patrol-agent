package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientToolMcpVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP 工具配置数据加载策略
 */
@Slf4j
@Service
public class AiClientToolMCPLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<AiClientToolMcpVO> mcpList = repository.AiClientToolMcpVOByClientIds(requestParameter.getCommandIdList());
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getDataName(), mcpList);
        log.info("装配数据加载完成，MCP 工具配置 {} 条", mcpList == null ? 0 : mcpList.size());
    }

}
