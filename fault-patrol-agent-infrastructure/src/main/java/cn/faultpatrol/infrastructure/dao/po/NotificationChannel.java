package cn.faultpatrol.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 告警通知渠道 PO 对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationChannel {

    private Long id;
    private String channelId;
    private String channelName;
    private String channelType;
    private String webhookUrl;
    private String secret;
    private String notifySeverities;
    private Integer status;
    private Date createTime;
    private Date updateTime;

}
