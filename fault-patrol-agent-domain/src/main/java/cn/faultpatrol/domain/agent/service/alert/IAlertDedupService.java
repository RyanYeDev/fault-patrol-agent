package cn.faultpatrol.domain.agent.service.alert;

/**
 * 告警去重服务接口
 * <p>
 * 相同告警在去重窗口内重复推送时合并处理，避免重复全量诊断。
 */
public interface IAlertDedupService {

    /**
     * 接收告警并记录指纹
     *
     * @param alertName     告警名称
     * @param severity      告警级别
     * @param source        告警来源
     * @param alertContent  告警内容
     * @param sessionId     本次诊断会话ID
     * @param windowMinutes 去重窗口（分钟）
     * @return 去重结果
     */
    AlertDedupResult accept(String alertName, String severity, String source,
                            String alertContent, String sessionId, int windowMinutes);

    /**
     * 去重结果
     */
    class AlertDedupResult {

        private final String fingerprint;
        private final int hitCount;
        private final boolean shouldDiagnose;

        public AlertDedupResult(String fingerprint, int hitCount, boolean shouldDiagnose) {
            this.fingerprint = fingerprint;
            this.hitCount = hitCount;
            this.shouldDiagnose = shouldDiagnose;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public int getHitCount() {
            return hitCount;
        }

        /**
         * 是否执行诊断：首次出现或超出窗口期为 true
         */
        public boolean isShouldDiagnose() {
            return shouldDiagnose;
        }
    }

}
