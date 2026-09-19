package cn.faultpatrol.domain.agent.service.armory.node;

import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientModelVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import cn.faultpatrol.domain.agent.service.armory.node.support.ToolTraceSupport;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话模型节点配置
 *
 */
@Slf4j
@Service
public class AiClientModelNode extends AbstractArmorySupport {

    @Resource
    private AiClientAdvisorNode aiClientAdvisorNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，Mode 对话模型{}", JSON.toJSONString(requestParameter));

        List<AiClientModelVO> aiClientModelList = dynamicContext.getValue(dataName());

        if (aiClientModelList == null || aiClientModelList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client model");
            return router(requestParameter, dynamicContext);
        }

        for (AiClientModelVO modelVO : aiClientModelList) {

            // 获取当前模型关联的 API Bean 对象
            OpenAiApi openAiApi = getBean(AiAgentEnumVO.AI_CLIENT_API.getBeanName(modelVO.getApiId()));
            if (null == openAiApi) {
                throw new RuntimeException("mode 2 api is null");
            }

            // 获取当前模型关联的 Tool MCP Bean 对象
            List<McpSyncClient> mcpSyncClients = new ArrayList<>();
            for (String toolMcpId : modelVO.getToolMcpIds()) {
                McpSyncClient mcpSyncClient = getBean(AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getBeanName(toolMcpId));
                mcpSyncClients.add(mcpSyncClient);
            }

            if ("ollama".equalsIgnoreCase(modelVO.getModelType())) {
                // Ollama 本地模型：baseUrl 复用 API 配置（如 http://localhost:11434），无需 apiKey
                String ollamaBaseUrl = findApiBaseUrl(dynamicContext, modelVO.getApiId());
                org.springframework.ai.ollama.api.OllamaApi ollamaApi =
                        org.springframework.ai.ollama.api.OllamaApi.builder()
                                .baseUrl(ollamaBaseUrl == null ? "http://localhost:11434" : ollamaBaseUrl)
                                .build();
                org.springframework.ai.ollama.OllamaChatModel ollamaChatModel =
                        org.springframework.ai.ollama.OllamaChatModel.builder()
                                .ollamaApi(ollamaApi)
                                .defaultOptions(org.springframework.ai.ollama.api.OllamaOptions.builder()
                                        .model(modelVO.getModelName())
                                        .build())
                                .build();
                registerBean(beanName(modelVO.getModelId()), org.springframework.ai.chat.model.ChatModel.class, ollamaChatModel);
                log.info("注册 Ollama 本地模型：{} -> {}", modelVO.getModelId(), modelVO.getModelName());
                continue;
            }

            // 实例化对话模型（OpenAI 兼容协议；其他兼容服务可通过 one-api 等网关统一为 openai 格式）
            // 工具回调在此包装调用轨迹采集：模型内部 ToolCallingManager 从模型选项解析回调
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(
                            OpenAiChatOptions.builder()
                                    .model(modelVO.getModelName())
                                    .toolCallbacks(ToolTraceSupport.wrap(
                                            new SyncMcpToolCallbackProvider(mcpSyncClients)).getToolCallbacks())
                                    .build())
                    .build();

            // 注册 Bean 对象
            registerBean(beanName(modelVO.getModelId()), OpenAiChatModel.class, chatModel);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientAdvisorNode;
    }

    /**
     * 从装配上下文的 API 配置中查找指定 apiId 的 baseUrl
     */
    private String findApiBaseUrl(DefaultArmoryStrategyFactory.DynamicContext dynamicContext, String apiId) {
        List<cn.faultpatrol.domain.agent.model.valobj.AiClientApiVO> apiList =
                dynamicContext.getValue(AiAgentEnumVO.AI_CLIENT_API.getDataName());
        if (apiList == null) {
            return null;
        }
        return apiList.stream()
                .filter(api -> apiId.equals(api.getApiId()))
                .map(cn.faultpatrol.domain.agent.model.valobj.AiClientApiVO::getBaseUrl)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_MODEL.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_MODEL.getDataName();
    }

}
