package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiClientAdvisorVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientApiVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientModelVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientSystemPromptVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientToolMcpVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientVO;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 客户端装配数据加载策略
 * <p>
 * 按客户端 ID 列表反查全部依赖组件配置：API、模型、MCP 工具、
 * 系统提示词、Advisor 与客户端本体，一次性载入动态上下文。
 */
@Slf4j
@Service
public class AiClientLoadDataStrategy implements ILoadDataStrategy {

    @Resource
    private IAgentRepository repository;

    @Override
    public void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<String> clientIds = requestParameter.getCommandIdList();

        List<AiClientApiVO> apiList = repository.queryAiClientApiVOListByClientIds(clientIds);
        List<AiClientModelVO> modelList = repository.AiClientModelVOByClientIds(clientIds);
        List<AiClientToolMcpVO> mcpList = repository.AiClientToolMcpVOByClientIds(clientIds);
        Map<String, AiClientSystemPromptVO> promptMap = repository.queryAiClientSystemPromptMapByClientIds(clientIds);
        List<AiClientAdvisorVO> advisorList = repository.AiClientAdvisorVOByClientIds(clientIds);
        List<AiClientVO> clientList = repository.AiClientVOByClientIds(clientIds);

        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_API.getDataName(), apiList);
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_MODEL.getDataName(), modelList);
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getDataName(), mcpList);
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_SYSTEM_PROMPT.getDataName(), promptMap);
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT_ADVISOR.getDataName(), advisorList);
        dynamicContext.setValue(AiAgentEnumVO.AI_CLIENT.getDataName(), clientList);

        log.info("装配数据加载完成，客户端 {} / API {} / 模型 {} / MCP {} / 提示词 {} / Advisor {}",
                clientList == null ? 0 : clientList.size(),
                apiList == null ? 0 : apiList.size(),
                modelList == null ? 0 : modelList.size(),
                mcpList == null ? 0 : mcpList.size(),
                promptMap == null ? 0 : promptMap.size(),
                advisorList == null ? 0 : advisorList.size());
    }

}
