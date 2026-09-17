package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 巡检诊断请求 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiagnoseRequestDTO implements Serializable {

    /**
     * 智能体ID
     */
    private String aiAgentId;

    /**
     * 诊断输入（告警描述或巡检指令）
     */
    private String message;

    /**
     * 会话ID（多轮追问时复用同一会话ID）
     */
    private String sessionId;

    /**
     * 最大执行步数
     */
    private Integer maxStep;

}
