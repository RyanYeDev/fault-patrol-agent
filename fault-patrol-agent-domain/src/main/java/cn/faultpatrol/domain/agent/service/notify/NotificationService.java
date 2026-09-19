package cn.faultpatrol.domain.agent.service.notify;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.model.valobj.NotificationChannelVO;
import cn.faultpatrol.types.enums.NotificationChannelTypeEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 故障告警与诊断报告多渠道通知中心实现
 */
@Slf4j
@Service
public class NotificationService implements INotificationService {

    @Resource
    private IAgentRepository repository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public void sendDiagnosisReportNotification(DiagnosisReportVO reportVO) {
        if (reportVO == null) return;

        List<NotificationChannelVO> channels = repository.queryActiveNotificationChannels();
        if (channels == null || channels.isEmpty()) {
            log.info("未配置活跃的通知渠道，跳过通知广播");
            return;
        }

        String markdownContent = buildReportMarkdown(reportVO);

        for (NotificationChannelVO channel : channels) {
            try {
                dispatchNotification(channel, "【巡检报告】" + StringUtils.defaultString(reportVO.getAlertContent(), "故障诊断已完成"), markdownContent, reportVO);
            } catch (Exception e) {
                log.error("渠道 [{}] 发送诊断通知异常：{}", channel.getChannelName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void sendRemediationApprovalNotification(String sessionId, String actionTitle, String riskLevel, String actionId) {
        List<NotificationChannelVO> channels = repository.queryActiveNotificationChannels();
        if (channels == null || channels.isEmpty()) {
            return;
        }

        String markdown = String.format("""
                ### ⚠️ 【高危处置审批提醒】
                - **诊断会话**: `%s`
                - **处置行动**: **%s**
                - **风险等级**: <font color="red">**%s**</font>
                - **动作ID**: `%s`
                > 提示：该处置动作包含生产变更操作，请运维人员进入控制台复核并审批。
                """, sessionId, actionTitle, riskLevel, actionId);

        for (NotificationChannelVO channel : channels) {
            try {
                dispatchNotification(channel, "【待审批提醒】" + actionTitle, markdown, null);
            } catch (Exception e) {
                log.error("渠道 [{}] 发送审批通知异常：{}", channel.getChannelName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void saveChannel(NotificationChannelVO channelVO) {
        repository.saveNotificationChannel(channelVO);
    }

    @Override
    public List<NotificationChannelVO> queryActiveChannels() {
        return repository.queryActiveNotificationChannels();
    }

    private void dispatchNotification(NotificationChannelVO channel, String title, String markdown, DiagnosisReportVO reportVO) throws Exception {
        NotificationChannelTypeEnum type = NotificationChannelTypeEnum.fromCode(channel.getChannelType());
        String targetUrl = channel.getWebhookUrl();
        String payloadJson = "";

        switch (type) {
            case DINGTALK:
                long timestamp = System.currentTimeMillis();
                if (StringUtils.isNotBlank(channel.getSecret())) {
                    String sign = signHmacSha256(timestamp + "\n" + channel.getSecret(), channel.getSecret());
                    targetUrl += (targetUrl.contains("?") ? "&" : "?") + "timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
                }
                JSONObject dingtalk = new JSONObject();
                dingtalk.put("msgtype", "markdown");
                JSONObject md = new JSONObject();
                md.put("title", title);
                md.put("text", markdown);
                dingtalk.put("markdown", md);
                payloadJson = dingtalk.toJSONString();
                break;

            case FEISHU:
                JSONObject feishu = new JSONObject();
                feishu.put("msg_type", "interactive");
                JSONObject card = new JSONObject();
                JSONObject header = new JSONObject();
                header.put("title", MapOf("tag", "plain_text", "content", "🚨 " + title));
                card.put("header", header);
                card.put("elements", List.of(MapOf("tag", "markdown", "content", markdown)));
                feishu.put("card", card);
                payloadJson = feishu.toJSONString();
                break;

            case WECOM:
                JSONObject wecom = new JSONObject();
                wecom.put("msgtype", "markdown");
                JSONObject wcmd = new JSONObject();
                wcmd.put("content", markdown);
                wecom.put("markdown", wcmd);
                payloadJson = wecom.toJSONString();
                break;

            case SLACK:
                JSONObject slack = new JSONObject();
                slack.put("text", markdown);
                payloadJson = slack.toJSONString();
                break;

            case GENERIC_WEBHOOK:
            default:
                JSONObject generic = new JSONObject();
                generic.put("event", "FAULT_PATROL_DIAGNOSIS_EVENT");
                generic.put("title", title);
                generic.put("content", markdown);
                if (reportVO != null) {
                    generic.put("report", reportVO);
                }
                payloadJson = generic.toJSONString();
                break;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("推送通知至渠道 [{}]，HTTP状态码: {}", channel.getChannelName(), response.statusCode());
    }

    private String buildReportMarkdown(DiagnosisReportVO report) {
        return String.format("""
                ### 🚨 【Fault Patrol 故障巡检诊断报告】
                - **会话ID**: `%s`
                - **诊断状态**: `%s`
                - **诊断时间**: `%s`
                
                #### 📌 原始告警
                > %s
                
                #### 🔍 根因分析
                %s
                
                #### 🛠️ 处置建议
                %s
                
                ---
                *由 Fault Patrol Agent 智能故障巡检平台全自动取证生成*
                """,
                report.getSessionId(),
                report.getStatus(),
                report.getCreateTime() != null ? report.getCreateTime().toString() : "刚刚",
                StringUtils.defaultIfBlank(report.getAlertContent(), "无告警详情"),
                StringUtils.defaultIfBlank(report.getRootCause(), "未定位到明确根因"),
                StringUtils.defaultIfBlank(report.getRemediation(), "请登录控制台复核排查"));
    }

    private String signHmacSha256(String stringToSign, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private java.util.Map<String, Object> MapOf(Object... kvs) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put(String.valueOf(kvs[i]), kvs[i + 1]);
        }
        return map;
    }

}
