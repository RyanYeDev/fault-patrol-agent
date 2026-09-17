package cn.faultpatrol.domain.agent.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface IRagService {

    /**
     * 知识库文件入库：解析 → 切块 → 向量化 → 写 pgvector → 台账落库
     *
     * @param name  知识库名称
     * @param tag   知识标签
     * @param files 文件列表
     */
    void storeRagFile(String name, String tag, List<MultipartFile> files);

    /**
     * 删除知识库文件：按知识标签 + 来源文件名删除向量数据与台账
     *
     * @param tag      知识标签
     * @param fileName 来源文件名（为空时删除该标签下全部文档）
     */
    void deleteRagFile(String tag, String fileName);

}
