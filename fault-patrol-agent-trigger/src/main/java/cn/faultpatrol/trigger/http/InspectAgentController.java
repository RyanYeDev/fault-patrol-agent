package cn.faultpatrol.trigger.http;

import cn.faultpatrol.api.IInspectAgentService;
import cn.faultpatrol.api.dto.AiAgentResponseDTO;
import cn.faultpatrol.api.dto.AlertRequestDTO;
import cn.faultpatrol.api.dto.ArmoryAgentRequestDTO;
import cn.faultpatrol.api.dto.ArmoryApiRequestDTO;
import cn.faultpatrol.api.dto.DiagnoseRequestDTO;
import cn.faultpatrol.api.dto.DiagnosisReportResponseDTO;
import cn.faultpatrol.api.response.Response;
import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.model.valobj.AiAgentVO;
import cn.faultpatrol.domain.agent.model.valobj.DiagnosisReportVO;
import cn.faultpatrol.domain.agent.service.IAgentDispatchService;
import cn.faultpatrol.domain.agent.service.IArmoryService;
import cn.faultpatrol.types.enums.ResponseCode;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检诊断控制器
 * <p>
 * 提供巡检诊断、告警接入（SSE 流式）、智能体装配与诊断报告查询能力。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inspect")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class InspectAgentController implements IInspectAgentService {

    @Resource
    private IAgentDispatchService agentDispatchService;

    @Resource
    private IArmoryService armoryService;

    /**
     * 告警接入默认巡检智能体ID
     */
    @Value("${faultpatrol.alert.default-agent-id:10001}")
    private String defaultAgentId;

    @RequestMapping(value = "diagnose", method = RequestMethod.POST)
    @Override
    public ResponseBodyEmitter diagnose(@RequestBody DiagnoseRequestDTO request, HttpServletResponse response) {
        log.info("巡检诊断请求开始，请求信息：{}", JSON.toJSONString(request));

        try {
            // 设置SSE响应头
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            // 1. 创建流式输出对象
            ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);

            // 2. 构建执行命令实体
            ExecuteCommandEntity executeCommandEntity = ExecuteCommandEntity.builder()
                    .aiAgentId(request.getAiAgentId())
                    .message(request.getMessage())
                    .sessionId(request.getSessionId())
                    .maxStep(request.getMaxStep())
                    .build();

            // 3. 调度处理
            agentDispatchService.dispatch(executeCommandEntity, emitter);

            return emitter;

        } catch (Exception e) {
            log.error("巡检诊断请求处理异常：{}", e.getMessage(), e);
            return errorEmitter("请求处理异常：" + e.getMessage());
        }
    }

    @RequestMapping(value = "alert", method = RequestMethod.POST)
    @Override
    public ResponseBodyEmitter alert(@RequestBody AlertRequestDTO request, HttpServletResponse response) {
        log.info("告警接入请求开始，请求信息：{}", JSON.toJSONString(request));

        try {
            // 设置SSE响应头
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            // 1. 创建流式输出对象
            ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);

            // 2. 组装告警诊断指令
            String aiAgentId = StringUtils.isBlank(request.getAiAgentId()) ? defaultAgentId : request.getAiAgentId();
            String message = buildAlertMessage(request);

            // 3. 构建执行命令实体
            ExecuteCommandEntity executeCommandEntity = ExecuteCommandEntity.builder()
                    .aiAgentId(aiAgentId)
                    .message(message)
                    .sessionId("alert_" + System.currentTimeMillis())
                    .maxStep(5)
                    .build();

            // 4. 调度处理
            agentDispatchService.dispatch(executeCommandEntity, emitter);

            return emitter;

        } catch (Exception e) {
            log.error("告警接入请求处理异常：{}", e.getMessage(), e);
            return errorEmitter("请求处理异常：" + e.getMessage());
        }
    }

    @RequestMapping(value = "armory_agent", method = RequestMethod.POST)
    @Override
    public Response<Boolean> armoryAgent(@RequestBody ArmoryAgentRequestDTO request) {
        log.info("装配智能体请求开始，请求信息：{}", JSON.toJSONString(request));

        try {
            // 参数校验
            if (request == null || request.getAgentId() == null || request.getAgentId().trim().isEmpty()) {
                log.warn("装配智能体请求参数无效：agentId为空");
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("agentId不能为空")
                        .data(false)
                        .build();
            }

            // 调用装配服务
            armoryService.acceptArmoryAgent(request.getAgentId());

            log.info("装配智能体成功，agentId：{}", request.getAgentId());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("装配成功")
                    .data(true)
                    .build();

        } catch (Exception e) {
            log.error("装配智能体失败，agentId：{}，错误信息：{}",
                    request != null ? request.getAgentId() : "null", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("装配失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    @RequestMapping(value = "available_agents", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentResponseDTO>> queryAvailableAgents() {
        log.info("查询可用智能体列表请求开始");

        try {
            // 调用装配服务查询可用智能体
            List<AiAgentVO> aiAgentVOList = armoryService.queryAvailableAgents();

            // 转换为响应DTO
            List<AiAgentResponseDTO> responseList = new ArrayList<>();
            for (AiAgentVO aiAgentVO : aiAgentVOList) {
                AiAgentResponseDTO responseDTO = AiAgentResponseDTO.builder()
                        .agentId(aiAgentVO.getAgentId())
                        .agentName(aiAgentVO.getAgentName())
                        .description(aiAgentVO.getDescription())
                        .channel(aiAgentVO.getChannel())
                        .strategy(aiAgentVO.getStrategy())
                        .status(aiAgentVO.getStatus())
                        .build();
                responseList.add(responseDTO);
            }

            log.info("查询可用智能体列表成功，共{}个智能体", responseList.size());
            return Response.<List<AiAgentResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(responseList)
                    .build();

        } catch (Exception e) {
            log.error("查询可用智能体列表失败，错误信息：{}", e.getMessage(), e);
            return Response.<List<AiAgentResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    @RequestMapping(value = "armory_api", method = RequestMethod.POST)
    @Override
    public Response<Boolean> armoryApi(@RequestBody ArmoryApiRequestDTO request) {
        log.info("装配API请求开始，请求信息：{}", JSON.toJSONString(request));

        try {
            // 参数校验
            if (request == null || request.getApiId() == null || request.getApiId().trim().isEmpty()) {
                log.warn("装配API请求参数无效：apiId为空");
                return Response.<Boolean>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("apiId不能为空")
                        .data(false)
                        .build();
            }

            // 调用装配服务
            armoryService.acceptArmoryAgentClientModelApi(request.getApiId());

            log.info("装配API成功，apiId：{}", request.getApiId());
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("装配成功")
                    .data(true)
                    .build();

        } catch (Exception e) {
            log.error("装配API失败，apiId：{}，错误信息：{}",
                    request != null ? request.getApiId() : "null", e.getMessage(), e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("装配失败：" + e.getMessage())
                    .data(false)
                    .build();
        }
    }

    @RequestMapping(value = "reports", method = RequestMethod.GET)
    @Override
    public Response<List<DiagnosisReportResponseDTO>> queryReports(@RequestParam String sessionId) {
        try {
            List<DiagnosisReportVO> reportVOS = armoryService.queryDiagnosisReportsBySessionId(sessionId);

            List<DiagnosisReportResponseDTO> responseList = new ArrayList<>();
            for (DiagnosisReportVO reportVO : reportVOS) {
                responseList.add(convertReportVO(reportVO));
            }

            return Response.<List<DiagnosisReportResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(responseList)
                    .build();
        } catch (Exception e) {
            log.error("查询诊断报告失败：{}", e.getMessage(), e);
            return Response.<List<DiagnosisReportResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .data(new ArrayList<>())
                    .build();
        }
    }

    @RequestMapping(value = "report/{id}", method = RequestMethod.GET)
    @Override
    public Response<DiagnosisReportResponseDTO> queryReportById(@PathVariable Long id) {
        try {
            DiagnosisReportVO reportVO = armoryService.queryDiagnosisReportById(id);
            if (reportVO == null) {
                return Response.<DiagnosisReportResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("报告不存在")
                        .build();
            }

            return Response.<DiagnosisReportResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("查询成功")
                    .data(convertReportVO(reportVO))
                    .build();
        } catch (Exception e) {
            log.error("查询诊断报告失败：{}", e.getMessage(), e);
            return Response.<DiagnosisReportResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 组装告警诊断指令
     */
    private String buildAlertMessage(AlertRequestDTO request) {
        return String.format("""
                【故障巡检告警】
                告警名称：%s
                告警级别：%s
                告警来源：%s
                告警内容：%s

                请对该告警进行故障定位：分析告警特征、制定取证计划，调用可用巡检工具
                （指标查询、链路追踪、业务数据、Redis、MQ、容器等）完成交叉取证，
                输出根因分析与处置建议。""",
                StringUtils.defaultString(request.getAlertName(), "未命名告警"),
                StringUtils.defaultString(request.getSeverity(), "warning"),
                StringUtils.defaultString(request.getSource(), "unknown"),
                StringUtils.defaultString(request.getAlertContent(), "无详细内容"));
    }

    private DiagnosisReportResponseDTO convertReportVO(DiagnosisReportVO reportVO) {
        return DiagnosisReportResponseDTO.builder()
                .id(reportVO.getId())
                .sessionId(reportVO.getSessionId())
                .agentId(reportVO.getAgentId())
                .alertContent(reportVO.getAlertContent())
                .rootCause(reportVO.getRootCause())
                .remediation(reportVO.getRemediation())
                .evidence(reportVO.getEvidence())
                .summary(reportVO.getSummary())
                .status(reportVO.getStatus())
                .createTime(reportVO.getCreateTime())
                .build();
    }

    /**
     * 构建错误 SSE 响应
     */
    private ResponseBodyEmitter errorEmitter(String message) {
        ResponseBodyEmitter errorEmitter = new ResponseBodyEmitter();
        try {
            errorEmitter.send("data: " + JSON.toJSONString(
                    cn.faultpatrol.domain.agent.model.entity.DiagnoseExecuteResultEntity.createErrorResult(message, null)) + "\n\n");
            errorEmitter.complete();
        } catch (Exception ex) {
            log.error("发送错误信息失败：{}", ex.getMessage(), ex);
        }
        return errorEmitter;
    }

}
