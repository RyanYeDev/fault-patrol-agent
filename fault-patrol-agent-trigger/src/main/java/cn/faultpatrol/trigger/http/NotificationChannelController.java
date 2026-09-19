package cn.faultpatrol.trigger.http;

import cn.faultpatrol.api.dto.NotificationChannelDTO;
import cn.faultpatrol.api.response.Response;
import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.model.valobj.NotificationChannelVO;
import cn.faultpatrol.domain.agent.service.notify.INotificationService;
import cn.faultpatrol.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 告警通知渠道管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inspect/notification")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class NotificationChannelController {

    @Resource
    private INotificationService notificationService;

    @Resource
    private IAgentRepository repository;

    @RequestMapping(value = "channels", method = RequestMethod.GET)
    public Response<List<NotificationChannelDTO>> listChannels() {
        try {
            List<NotificationChannelVO> list = repository.queryAllNotificationChannels();
            List<NotificationChannelDTO> dtos = list.stream().map(this::toDTO).collect(Collectors.toList());
            return Response.<List<NotificationChannelDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("查询通知渠道列表异常：{}", e.getMessage(), e);
            return Response.<List<NotificationChannelDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    @RequestMapping(value = "channel", method = RequestMethod.POST)
    public Response<Boolean> saveChannel(@RequestBody NotificationChannelDTO request) {
        try {
            if (request == null || request.getWebhookUrl() == null || request.getChannelName() == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("渠道名称和Webhook地址不能为空")
                        .data(false)
                        .build();
            }

            String channelId = request.getChannelId();
            if (channelId == null || channelId.trim().isEmpty()) {
                channelId = "chan_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            }

            NotificationChannelVO vo = NotificationChannelVO.builder()
                    .channelId(channelId)
                    .channelName(request.getChannelName())
                    .channelType(request.getChannelType() != null ? request.getChannelType() : "GENERIC_WEBHOOK")
                    .webhookUrl(request.getWebhookUrl())
                    .secret(request.getSecret())
                    .notifySeverities(request.getNotifySeverities() != null ? request.getNotifySeverities() : "critical,warning")
                    .status(request.getStatus() != null ? request.getStatus() : 1)
                    .build();

            notificationService.saveChannel(vo);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("保存成功")
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("保存通知渠道异常：{}", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("保存失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    @RequestMapping(value = "channel/status", method = RequestMethod.POST)
    public Response<Boolean> updateStatus(@RequestParam String channelId, @RequestParam Integer status) {
        try {
            repository.updateNotificationChannelStatus(channelId, status);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("状态更新成功")
                    .data(true)
                    .build();
        } catch (Exception e) {
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("更新失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    @RequestMapping(value = "channel/test", method = RequestMethod.POST)
    public Response<Boolean> testChannel(@RequestParam String channelId) {
        try {
            NotificationChannelVO channel = repository.queryNotificationChannelById(channelId);
            if (channel == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("渠道不存在")
                        .data(false)
                        .build();
            }

            DiagnosisReportVO testReport = DiagnosisReportVO.builder()
                    .sessionId("test_" + System.currentTimeMillis())
                    .alertContent("【测试连通性】Fault Patrol 通知渠道测试")
                    .rootCause("测试演练，无实际故障")
                    .remediation("这是一条通道健康连通性测试消息，无需处置。")
                    .status("TEST")
                    .createTime(new java.util.Date())
                    .build();

            notificationService.sendDiagnosisReportNotification(testReport);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("测试通知已发送")
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("测试通知渠道失败：{}", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("测试失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    private NotificationChannelDTO toDTO(NotificationChannelVO vo) {
        if (vo == null) return null;
        return NotificationChannelDTO.builder()
                .channelId(vo.getChannelId())
                .channelName(vo.getChannelName())
                .channelType(vo.getChannelType())
                .webhookUrl(vo.getWebhookUrl())
                .secret(vo.getSecret())
                .notifySeverities(vo.getNotifySeverities())
                .status(vo.getStatus())
                .createTime(vo.getCreateTime())
                .build();
    }

}
