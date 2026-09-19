package cn.faultpatrol.domain.agent.service.notify;

import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.model.valobj.NotificationChannelVO;

import java.util.List;

/**
 * 故障告警与诊断报告多渠道通知中心服务
 */
public interface INotificationService {

    /**
     * 诊断报告生成完毕后触发多渠道通知广播
     */
    void sendDiagnosisReportNotification(DiagnosisReportVO reportVO);

    /**
     * 发送高危处置审批提醒通知
     */
    void sendRemediationApprovalNotification(String sessionId, String actionTitle, String riskLevel, String actionId);

    /**
     * 添加/更新通知渠道配置
     */
    void saveChannel(NotificationChannelVO channelVO);

    /**
     * 查询所有启用的通知渠道
     */
    List<NotificationChannelVO> queryActiveChannels();

}
