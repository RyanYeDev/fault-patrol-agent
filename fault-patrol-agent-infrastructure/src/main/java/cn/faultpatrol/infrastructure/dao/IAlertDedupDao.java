package cn.faultpatrol.infrastructure.dao;

import cn.faultpatrol.infrastructure.dao.po.AlertDedup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 告警去重记录表 DAO
 */
@Mapper
public interface IAlertDedupDao {

    /**
     * 去重窗口内命中计数（首次/超窗重置为 1）
     *
     * @param alertDedup    去重记录
     * @param windowMinutes 去重窗口（分钟）
     * @return 影响行数
     */
    int upsert(@Param("alertDedup") AlertDedup alertDedup, @Param("windowMinutes") int windowMinutes);

    /**
     * 查询当前命中次数
     *
     * @param fingerprint 告警指纹
     * @return 命中次数
     */
    int queryHitCount(@Param("fingerprint") String fingerprint);

}
