package cn.faultpatrol.domain.agent.service.alert;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.types.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 告警去重服务实现
 */
@Slf4j
@Service
public class AlertDedupService implements IAlertDedupService {

    @Resource
    private IAgentRepository repository;

    @Override
    public AlertDedupResult accept(String alertName, String severity, String source,
                                   String alertContent, String sessionId, int windowMinutes) {
        String fingerprint = SecurityUtil.fingerprint(alertName, severity, alertContent);
        int hitCount = repository.recordAlert(fingerprint, alertName, severity, source,
                sessionId, windowMinutes);
        boolean shouldDiagnose = hitCount <= 1;
        log.info("告警指纹 {} 命中 {} 次，{}", fingerprint, hitCount,
                shouldDiagnose ? "执行诊断" : "窗口期内重复，跳过诊断");
        return new AlertDedupResult(fingerprint, hitCount, shouldDiagnose);
    }

}
