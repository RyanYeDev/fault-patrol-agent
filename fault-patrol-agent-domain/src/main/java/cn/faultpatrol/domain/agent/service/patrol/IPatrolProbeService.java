package cn.faultpatrol.domain.agent.service.patrol;

import cn.faultpatrol.domain.agent.model.valobj.PatrolTargetVO;

import java.util.List;

/**
 * 微服务主动巡检探针服务接口
 */
public interface IPatrolProbeService {

    /**
     * 探针检查结果实体
     */
    record ProbeResult(boolean healthy, String status, String message, long latencyMs) {}

    /**
     * 对指定监控目标执行一次探针检测
     */
    ProbeResult probe(PatrolTargetVO target);

    /**
     * 执行全量活跃目标的探针巡检，若发现异常自动联动诊断 Agent
     */
    void runAllActiveProbes();

    /**
     * 注册或更新微服务监控目标
     */
    void registerTarget(PatrolTargetVO targetVO);

    /**
     * 查询所有监控目标列表
     */
    List<PatrolTargetVO> queryAllTargets();

}
