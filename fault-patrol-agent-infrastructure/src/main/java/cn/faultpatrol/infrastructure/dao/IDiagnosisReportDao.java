package cn.faultpatrol.infrastructure.dao;

import cn.faultpatrol.infrastructure.dao.po.DiagnosisReport;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 诊断报告表 DAO
 */
@Mapper
public interface IDiagnosisReportDao {

    /**
     * 插入诊断报告
     *
     * @param diagnosisReport 诊断报告对象
     * @return 影响行数
     */
    int insert(DiagnosisReport diagnosisReport);

    /**
     * 根据ID查询诊断报告
     *
     * @param id 主键ID
     * @return 诊断报告对象
     */
    DiagnosisReport queryById(Long id);

    /**
     * 根据会话ID查询诊断报告列表
     *
     * @param sessionId 会话ID
     * @return 诊断报告列表
     */
    List<DiagnosisReport> queryBySessionId(String sessionId);

    /**
     * 分页查询最近的诊断报告列表
     *
     * @return 诊断报告列表
     */
    List<DiagnosisReport> queryRecentReports();

}
