package cn.faultpatrol.api;

import cn.faultpatrol.api.dto.AiAgentResponseDTO;
import cn.faultpatrol.api.dto.AlertRequestDTO;
import cn.faultpatrol.api.dto.ArmoryAgentRequestDTO;
import cn.faultpatrol.api.dto.ArmoryApiRequestDTO;
import cn.faultpatrol.api.dto.DiagnoseRequestDTO;
import cn.faultpatrol.api.dto.DiagnosisReportResponseDTO;
import cn.faultpatrol.api.response.Response;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * 巡检诊断服务接口
 * <p>
 * 提供巡检诊断（SSE 流式）、告警接入（SSE 流式）、
 * 诊断报告查询与智能体装配能力。
 */
public interface IInspectAgentService {

    /**
     * 巡检诊断：Plan-and-Execute 四阶段流式诊断
     *
     * @param request  诊断请求
     * @param response HTTP 响应
     * @return SSE 流式输出
     */
    ResponseBodyEmitter diagnose(DiagnoseRequestDTO request, HttpServletResponse response);

    /**
     * 告警接入：告警 webhook 自动发起巡检诊断
     *
     * @param rawBody   原始请求体（用于 HMAC 签名校验）
     * @param signature 请求头 X-Webhook-Signature（可空）
     * @param response  HTTP 响应
     * @return SSE 流式输出
     */
    ResponseBodyEmitter alert(String rawBody, String signature, HttpServletResponse response);

    /**
     * 装配智能体
     *
     * @param request 装配请求
     * @return 装配结果
     */
    Response<Boolean> armoryAgent(ArmoryAgentRequestDTO request);

    /**
     * 查询可用智能体列表
     *
     * @return 智能体列表
     */
    Response<List<AiAgentResponseDTO>> queryAvailableAgents();

    /**
     * 装配 API
     *
     * @param request 装配请求
     * @return 装配结果
     */
    Response<Boolean> armoryApi(ArmoryApiRequestDTO request);

    /**
     * 按会话查询诊断报告
     *
     * @param sessionId 会话ID
     * @return 诊断报告列表
     */
    Response<List<DiagnosisReportResponseDTO>> queryReports(String sessionId);

    /**
     * 按ID查询诊断报告
     *
     * @param id 报告ID
     * @return 诊断报告
     */
    Response<DiagnosisReportResponseDTO> queryReportById(Long id);

    /**
     * 查询最近的诊断报告列表
     *
     * @return 诊断报告列表（最多 50 条）
     */
    Response<List<DiagnosisReportResponseDTO>> queryRecentReports();

}
