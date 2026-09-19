package cn.faultpatrol.infrastructure.dao;

import cn.faultpatrol.infrastructure.dao.po.NotificationChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知渠道 DAO
 */
@Mapper
public interface INotificationChannelDao {

    int insert(NotificationChannel channel);

    NotificationChannel queryByChannelId(@Param("channelId") String channelId);

    List<NotificationChannel> queryActiveChannels();

    List<NotificationChannel> queryAll();

    int updateStatus(@Param("channelId") String channelId, @Param("status") Integer status);

}
