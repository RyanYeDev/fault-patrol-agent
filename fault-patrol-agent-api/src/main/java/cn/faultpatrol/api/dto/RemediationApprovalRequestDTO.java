package cn.faultpatrol.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 处置动作审批请求 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemediationApprovalRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处置动作唯一ID
     */
    private String actionId;

    /**
     * 审批决定：APPROVED / REJECTED
     */
    private String decision;

    /**
     * 审批人姓名或工号
     */
    private String approver;

    /**
     * 审批批注或驳回原因
     */
    private String comment;

}
