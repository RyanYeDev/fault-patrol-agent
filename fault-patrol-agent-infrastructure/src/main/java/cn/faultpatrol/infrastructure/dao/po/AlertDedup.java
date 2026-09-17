package cn.faultpatrol.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警去重记录表 PO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertDedup {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 告警指纹（SHA-256）
     */
    private String fingerprint;

    /**
     * 告警名称
     */
    private String alertName;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 告警来源
     */
    private String source;

    /**
     * 首次出现时间
     */
    private LocalDateTime firstSeen;

    /**
     * 最近出现时间
     */
    private LocalDateTime lastSeen;

    /**
     * 去重窗口内命中次数
     */
    private Integer hitCount;

    /**
     * 最近一次诊断会话ID
     */
    private String lastSessionId;

}
