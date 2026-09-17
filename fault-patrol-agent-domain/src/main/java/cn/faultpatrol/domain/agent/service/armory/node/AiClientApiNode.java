package cn.faultpatrol.domain.agent.service.armory.node;

import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.faultpatrol.domain.agent.model.valobj.AiClientApiVO;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OpenAI API配置节点
 *
 * 2025/7/1 07:09
 */
@Slf4j
@Service
public class AiClientApiNode extends AbstractArmorySupport {

    @Resource
    private AiClientToolMcpNode aiClientToolMcpNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，API 接口请求{}", JSON.toJSONString(requestParameter));

        List<AiClientApiVO> aiClientApiList = dynamicContext.getValue(dataName());

        if (aiClientApiList == null || aiClientApiList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client api");
            return router(requestParameter, dynamicContext);
        }

        for (AiClientApiVO aiClientApiVO : aiClientApiList) {
            // 构建 OpenAiApi；通过 RestClient 拦截器为 deepseek 关闭 thinking 模式（避免多轮对话 reasoning_content 回传校验失败）
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(aiClientApiVO.getBaseUrl())
                    .apiKey(aiClientApiVO.getApiKey())
                    .completionsPath(aiClientApiVO.getCompletionsPath())
                    .embeddingsPath(aiClientApiVO.getEmbeddingsPath())
                    .restClientBuilder(RestClient.builder().requestInterceptor((request, body, execution) -> {
                        if (body != null && body.length > 0) {
                            try {
                                JSONObject obj = JSON.parseObject(new String(body, StandardCharsets.UTF_8));
                                if (obj != null && obj.containsKey("messages")) {
                                    JSONObject thinking = new JSONObject();
                                    thinking.put("type", "disabled");
                                    obj.put("thinking", thinking);
                                    body = obj.toJSONString().getBytes(StandardCharsets.UTF_8);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        return execution.execute(request, body);
                    }))
                    .build();

            // 注册 OpenAiApi Bean 对象
            registerBean(beanName(aiClientApiVO.getApiId()), OpenAiApi.class, openAiApi);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientToolMcpNode;
    }

    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_API.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_API.getDataName();
    }

}
