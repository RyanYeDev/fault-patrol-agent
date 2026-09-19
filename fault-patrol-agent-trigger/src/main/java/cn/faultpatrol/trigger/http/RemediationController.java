package cn.faultpatrol.trigger.http;

import cn.faultpatrol.api.dto.RemediationActionDTO;
import cn.faultpatrol.api.dto.RemediationApprovalRequestDTO;
import cn.faultpatrol.api.response.Response;
import cn.faultpatrol.domain.agent.model.valobj.RemediationActionVO;
import cn.faultpatrol.domain.agent.service.remediation.IRemediationService;
import cn.faultpatrol.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 故障处置动作与人机协同（HITL）审批控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inspect/remediation")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class RemediationController {

    @Resource
    private IRemediationService remediationService;

    /**
     * 根据会话ID查询关联的处置行动列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public Response<List<RemediationActionDTO>> listBySessionId(@RequestParam String sessionId) {
        try {
            List<RemediationActionVO> actionVOS = remediationService.queryBySessionId(sessionId);
            List<RemediationActionDTO> dtoList = actionVOS.stream().map(this::toDTO).collect(Collectors.toList());
            return Response.<List<RemediationActionDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(dtoList)
                    .build();
        } catch (Exception e) {
            log.error("查询处置动作失败：{}", e.getMessage(), e);
            return Response.<List<RemediationActionDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 查询待审批的处置动作列表
     */
    @RequestMapping(value = "pending", method = RequestMethod.GET)
    public Response<List<RemediationActionDTO>> pendingActions() {
        try {
            List<RemediationActionVO> actionVOS = remediationService.queryPendingActions();
            List<RemediationActionDTO> dtoList = actionVOS.stream().map(this::toDTO).collect(Collectors.toList());
            return Response.<List<RemediationActionDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(dtoList)
                    .build();
        } catch (Exception e) {
            log.error("查询待审批处置动作失败：{}", e.getMessage(), e);
            return Response.<List<RemediationActionDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    /**
     * 审批处置动作（通过或驳回）
     */
    @RequestMapping(value = "approve", method = RequestMethod.POST)
    public Response<Boolean> approveAction(@RequestBody RemediationApprovalRequestDTO request) {
        try {
            if (request == null || request.getActionId() == null) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("actionId不能为空")
                        .data(false)
                        .build();
            }

            boolean isApprove = "APPROVED".equalsIgnoreCase(request.getDecision());
            boolean ok;
            if (isApprove) {
                ok = remediationService.approveAction(request.getActionId(), request.getApprover(), request.getComment());
            } else {
                ok = remediationService.rejectAction(request.getActionId(), request.getApprover(), request.getComment());
            }

            return Response.<Boolean>builder()
                    .code(ok ? ResponseCode.SUCCESS.getCode() : ResponseCode.UN_ERROR.getCode())
                    .info(ok ? (isApprove ? "审批通过" : "已成功驳回") : "操作失败，可能动作不存在或状态不可变更")
                    .data(ok)
                    .build();
        } catch (Exception e) {
            log.error("审批处置动作异常：{}", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("审批异常：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    /**
     * 执行处置动作（支持 dryRun=true 演练模式）
     */
    @RequestMapping(value = "execute", method = RequestMethod.POST)
    public Response<RemediationActionDTO> executeAction(@RequestParam String actionId,
                                                        @RequestParam(defaultValue = "true") boolean dryRun) {
        try {
            RemediationActionVO actionVO = remediationService.executeAction(actionId, dryRun);
            return Response.<RemediationActionDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(dryRun ? "演练完成" : "执行完毕")
                    .data(toDTO(actionVO))
                    .build();
        } catch (IllegalStateException ise) {
            return Response.<RemediationActionDTO>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                    .info(ise.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("执行处置动作异常：{}", e.getMessage(), e);
            return Response.<RemediationActionDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("执行异常：" + e.getMessage())
                    .build();
        }
    }

    private RemediationActionDTO toDTO(RemediationActionVO vo) {
        if (vo == null) return null;
        return RemediationActionDTO.builder()
                .actionId(vo.getActionId())
                .sessionId(vo.getSessionId())
                .title(vo.getTitle())
                .actionType(vo.getActionType())
                .targetResource(vo.getTargetResource())
                .riskLevel(vo.getRiskLevel())
                .command(vo.getCommand())
                .rollbackPlan(vo.getRollbackPlan())
                .status(vo.getStatus())
                .approvalComment(vo.getApprovalComment())
                .approvedBy(vo.getApprovedBy())
                .executionLog(vo.getExecutionLog())
                .createTime(vo.getCreateTime())
                .updateTime(vo.getUpdateTime())
                .build();
    }

}
