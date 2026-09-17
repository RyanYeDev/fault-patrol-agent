package cn.faultpatrol.domain.agent.service.armory.node.factory.element;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagAnswerAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RagAnswerAdvisor.class);

    /** 检索 query 最大字符数：SiliconFlow bge-m3 输入上限 8192 token，超长会返回 HTTP 400(20015) */
    private static final int MAX_QUERY_CHARS = 2000;

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final String userTextAdvise;

    public RagAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.userTextAdvise = "\n参考资料 Context information is below，以 ===== 包围。\n\n=====\n{question_answer_context}\n=====\n\n回答规则（必须严格遵守）：\n1. 只能依据上方参考资料回答，禁止使用无关先验知识编造内容。\n2. 强制来源标注：回答中的每一条结论、要点或数据之后，必须紧跟【来源：文档标题】；标题只能取自上文每个片段标注的【来源：xxx】，严禁编造来源。\n3. 若参考资料不足以回答用户问题，请直接回复：根据知识库无法回答该问题。\n";
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        HashMap<String, Object> context = new HashMap(chatClientRequest.context());

        String userText = chatClientRequest.prompt().getUserMessage().getText();
        if (userText == null) {
            userText = "";
        }
        // 检索 query 仅用于向量召回；总结阶段会把完整执行历史拼进 prompt，超长会触发 embedding 400，这里截断保护
        String queryText = userText.length() > MAX_QUERY_CHARS ? userText.substring(0, MAX_QUERY_CHARS) : userText;
        if (!queryText.equals(userText)) {
            log.warn("RAG 检索 query 超长，已截断至 {} 字符（原 {} 字符）", MAX_QUERY_CHARS, userText.length());
        }
        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest).query(queryText).filterExpression(this.doGetFilterExpression(context)).build();
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
        context.put("qa_retrieved_documents", documents);

        // 组装带【来源：标题】标注的上下文，供模型逐条引用
        String documentContext = buildContextWithSources(documents);
        Map<String, Object> advisedUserParams = new HashMap(chatClientRequest.context());
        advisedUserParams.put("question_answer_context", documentContext);

        // 渲染提示词：参考资料真实注入 + 强制来源标注规则
        String advisedUserText = userText + System.lineSeparator() + new PromptTemplate(this.userTextAdvise).render(advisedUserParams);

        // 注意：不能往 messages 里伪造 assistant 消息。
        // deepseek-v4-flash 处于 thinking 模式时，流式请求要求历史中的 assistant 消息必须回传 reasoning_content，
        // 伪造的 assistant 消息（无 reasoning_content）会触发 400；参考资料已全部注入上方 user 消息即可。
        return ChatClientRequest.builder()
                .prompt(Prompt.builder().messages(new UserMessage(advisedUserText)).build())
                .context(advisedUserParams)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        chatResponseBuilder.metadata("qa_retrieved_documents", chatClientResponse.context().get("qa_retrieved_documents"));
        ChatResponse chatResponse = chatResponseBuilder.build();

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(this.before(chatClientRequest, callAdvisorChain));
        return this.after(chatClientResponse, callAdvisorChain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // call() 与 stream() 都先执行知识库检索，保证流式输出同样基于参考资料生成
        ChatClientRequest advisedRequest = this.before(chatClientRequest, streamAdvisorChain);
        return streamAdvisorChain.nextStream(advisedRequest);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString()) ? (new FilterExpressionTextParser()).parse(context.get("qa_filter_expression").toString()) : this.searchRequest.getFilterExpression();
    }

    /**
     * 组装带来源标注的检索上下文
     */
    private String buildContextWithSources(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "（知识库未检索到相关内容）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String source = resolveSource(doc);
            sb.append("[").append(i + 1).append("] 【来源：").append(source).append("】").append(System.lineSeparator())
              .append(doc.getText()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * 解析文档来源：优先 title，依次回退 file_name/source/knowledge
     */
    private String resolveSource(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        if (metadata == null) {
            return "未知来源";
        }
        for (String key : new String[]{"title", "file_name", "source", "knowledge"}) {
            Object value = metadata.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return "未知来源";
    }

}
