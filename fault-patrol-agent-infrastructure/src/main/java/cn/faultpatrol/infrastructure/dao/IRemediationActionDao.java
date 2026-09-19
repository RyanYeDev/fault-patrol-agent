package cn.faultpatrol.infrastructure.dao;

import cn.faultpatrol.infrastructure.dao.po.RemediationAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 故障处置行动 DAO
 */
@Mapper
public interface IRemediationActionDao {

    int insert(RemediationAction action);

    RemediationAction queryByActionId(@Param("actionId") String actionId);

    List<RemediationAction> queryBySessionId(@Param("sessionId") String sessionId);

    List<RemediationAction> queryPendingActions();

    int updateStatusAndApproval(RemediationAction action);

    int updateExecutionResult(RemediationAction action);

}
