package cn.faultpatrol.infrastructure.dao;

import cn.faultpatrol.infrastructure.dao.po.PatrolTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 巡检监控目标 DAO
 */
@Mapper
public interface IPatrolTargetDao {

    int insert(PatrolTarget target);

    PatrolTarget queryById(@Param("id") Long id);

    PatrolTarget queryByServiceName(@Param("serviceName") String serviceName);

    List<PatrolTarget> queryActiveTargets();

    List<PatrolTarget> queryAll();

    int updateCheckStatus(PatrolTarget target);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

}
