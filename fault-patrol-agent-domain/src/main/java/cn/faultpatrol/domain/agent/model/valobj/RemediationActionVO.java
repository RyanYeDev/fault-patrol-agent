package cn.faultpatrol.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 故障处置动作值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemediationActionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String actionId;
    private String sessionId;
    private String title;
    private String actionType;
    private String targetResource;
    private String riskLevel;
    private String command;
    private String rollbackPlan;
    private String status;
    private String approvalComment;
    private String approvedBy;
    private String executionLog;
    private Date createTime;
    private Date updateTime;

}
