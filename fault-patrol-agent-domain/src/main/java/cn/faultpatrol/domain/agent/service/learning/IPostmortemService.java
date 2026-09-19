package cn.faultpatrol.domain.agent.service.learning;

import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;

/**
 * 故障诊断经验自主学习与复盘服务（Hermes 自进化学习闭环）
 */
public interface IPostmortemService {

    /**
     * 针对已完成的成功诊断报告，自动生成故障排查复盘手册并沉淀至 RAG 知识库
     *
     * @param reportVO 诊断报告对象
     * @return 生成的 Postmortem Markdown 文本；若不满足沉淀条件则返回 null
     */
    String learnAndSynthesizePlaybook(DiagnosisReportVO reportVO);

}
