package cn.faultpatrol.domain.agent.service.execute.diagnose.step.support;

/**
 * 诊断报告小节提取器
 * <p>
 * 从 Markdown 报告中提取指定标题（如「根因分析」「处置建议」）下的内容：
 * 以标题行开始，遇到同级或更高级标题（# 或 ##）终止；### 及以下视为内容。
 */
public final class ReportSectionExtractor {

    private ReportSectionExtractor() {
    }

    /**
     * 提取报告中的指定小节内容
     *
     * @param report      报告全文
     * @param sectionName 小节标题（如「根因分析」）
     * @return 小节内容（不含标题行）；未找到时返回空字符串
     */
    public static String extractSection(String report, String sectionName) {
        if (report == null || report.isBlank()) {
            return "";
        }
        String[] lines = report.split("\n");
        StringBuilder content = new StringBuilder();
        boolean inSection = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^#{1,4}\\s*.*" + sectionName + ".*$")) {
                inSection = true;
                continue;
            }
            if (inSection) {
                // 遇到同级或更高级标题（# 或 ##）结束本小节；### 及以下视为内容
                if (trimmed.matches("^#{1,2}\\s+.*")) {
                    break;
                }
                if (trimmed.isBlank()) {
                    continue;
                }
                content.append(trimmed).append("\n");
            }
        }
        return content.toString().trim();
    }

}
