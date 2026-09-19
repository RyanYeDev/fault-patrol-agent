package cn.faultpatrol.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 故障处置动作 PO 对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemediationAction {

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
