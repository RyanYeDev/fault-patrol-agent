package cn.faultpatrol.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 巡检监控目标值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatrolTargetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String serviceName;
    private String probeType;
    private String targetEndpoint;
    private String thresholdConfig;
    private String intervalCron;
    private String aiAgentId;
    private Integer quietWindowMinutes;
    private Integer status;
    private Date lastCheckTime;
    private String lastCheckStatus;
    private String lastErrorMsg;
    private Date createTime;
    private Date updateTime;

}
