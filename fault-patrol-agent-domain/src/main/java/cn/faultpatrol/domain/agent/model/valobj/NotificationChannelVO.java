package cn.faultpatrol.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知渠道值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
