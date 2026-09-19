package cn.faultpatrol.domain.agent.service.rag;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.AiRagOrderVO;
import cn.faultpatrol.domain.agent.service.IRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 知识库服务
 * <p>
 * 入库：Tika 解析 → TokenTextSplitter 切块 → 打知识标签与来源元数据 → pgvector 向量化存储 → MySQL 台账
 * 删除：按知识标签 + 来源文件过滤删除向量数据与台账，支持覆盖式更新
 */
@Slf4j
@Service
public class RagService implements IRagService {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore vectorStore;

    @Resource
    private IAgentRepository repository;

    @Override
    public void storeRagFile(String name, String tag, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documentList = tokenTextSplitter.apply(documentReader.get());

            // 添加知识标签与来源文件元数据（删除/更新时按此过滤）
            String sourceName = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename() : file.getName();
            documentList.forEach(doc -> {
                doc.getMetadata().put("knowledge", tag);
                doc.getMetadata().put("source", sourceName);
            });

            // 存储知识库文件
            vectorStore.accept(documentList);

            // 存储到数据库台账（回填知识库ID）
            AiRagOrderVO aiRagOrderVO = new AiRagOrderVO();
            aiRagOrderVO.setRagId(UUID.randomUUID().toString().replace("-", ""));
            aiRagOrderVO.setRagName(name);
            aiRagOrderVO.setKnowledgeTag(tag);
            repository.createTagOrder(aiRagOrderVO);
        }
    }

    @Override
    public void deleteRagFile(String tag, String fileName) {
        // 构建删除过滤条件：knowledge == tag [AND source == fileName]
        Filter.Expression expression;
        if (StringUtils.hasText(fileName)) {
            expression = new Filter.Expression(Filter.ExpressionType.AND,
                    new Filter.Expression(Filter.ExpressionType.EQ,
                            new Filter.Key("knowledge"), new Filter.Value(tag)),
                    new Filter.Expression(Filter.ExpressionType.EQ,
                            new Filter.Key("source"), new Filter.Value(fileName)));
        } else {
            expression = new Filter.Expression(Filter.ExpressionType.EQ,
                    new Filter.Key("knowledge"), new Filter.Value(tag));
        }

        vectorStore.delete(expression);
        repository.deleteRagOrder(tag, fileName);
        log.info("知识库删除完成：tag {}，fileName {}", tag, fileName);
    }

    @Override
    public void storeTextContent(String name, String tag, String content, String sourceName) {
        if (!StringUtils.hasText(content)) return;
        Document document = new Document(content);
        List<Document> splitDocs = tokenTextSplitter.apply(List.of(document));
        splitDocs.forEach(doc -> {
            doc.getMetadata().put("knowledge", tag);
            doc.getMetadata().put("source", StringUtils.hasText(sourceName) ? sourceName : name);
        });
        vectorStore.accept(splitDocs);

        AiRagOrderVO aiRagOrderVO = new AiRagOrderVO();
        aiRagOrderVO.setRagId(UUID.randomUUID().toString().replace("-", ""));
        aiRagOrderVO.setRagName(name);
        aiRagOrderVO.setKnowledgeTag(tag);
        repository.createTagOrder(aiRagOrderVO);
        log.info("知识库文本沉淀入库完成：title={}, tag={}, chunks={}", name, tag, splitDocs.size());
    }

}
