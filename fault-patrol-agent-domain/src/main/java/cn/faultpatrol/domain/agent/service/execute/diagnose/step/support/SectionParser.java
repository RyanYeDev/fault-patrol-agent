package cn.faultpatrol.domain.agent.service.execute.diagnose.step.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阶段输出分节解析器
 * <p>
 * 将 LLM 按「小节标题:」格式输出的文本解析为有序的小节映射。
 * 每个阶段节点按自己的小节清单调用 {@link #parse(String, Collection)}，
 * 解析结果为空时由调用方降级处理（整段输出作为主事件发送）。
 */
public final class SectionParser {

    private SectionParser() {
    }

    /**
     * 解析文本中的小节
     *
     * @param text          LLM 输出文本
     * @param sectionNames 小节标题清单（按出现顺序匹配，如「故障分析」「执行历史评估」）
     * @return 有序的小节映射（标题 → 内容，不含标题行本身）
     */
    public static Map<String, String> parse(String text, Collection<String> sectionNames) {
        Map<String, String> sections = new LinkedHashMap<>();
        if (text == null || text.isBlank() || sectionNames == null || sectionNames.isEmpty()) {
            return sections;
        }

        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 命中小节标题（如「故障分析:」或「故障分析：」）
            String matched = matchSection(line, sectionNames);
            if (matched != null) {
                flush(sections, currentSection, currentContent);
                currentSection = matched;
                currentContent = new StringBuilder();
                // 标题行后面的同段内容（如「完成度评估: 85%」）一并保留
                String rest = extractRestAfterHeader(line);
                if (rest != null && !rest.isBlank()) {
                    currentContent.append(rest).append("\n");
                }
                continue;
            }

            if (currentSection != null) {
                currentContent.append(line).append("\n");
            }
        }
        flush(sections, currentSection, currentContent);

        return sections;
    }

    /**
     * 判断一行是否为某个小节的标题行
     */
    private static String matchSection(String line, Collection<String> sectionNames) {
        for (String name : sectionNames) {
            if (line.contains(name + ":") || line.contains(name + "：")) {
                return name;
            }
        }
        return null;
    }

    /**
     * 提取标题行冒号后的剩余内容（单行小节内容，如「完成度评估: 85%」→「85%」）
     */
    private static String extractRestAfterHeader(String line) {
        int ascii = line.indexOf(':');
        int fullWidth = line.indexOf('：');
        int idx;
        if (ascii >= 0 && fullWidth >= 0) {
            idx = Math.min(ascii, fullWidth);
        } else {
            idx = Math.max(ascii, fullWidth);
        }
        if (idx < 0 || idx + 1 >= line.length()) {
            return null;
        }
        return line.substring(idx + 1).trim();
    }

    private static void flush(Map<String, String> sections, String currentSection, StringBuilder content) {
        if (currentSection != null) {
            String text = content.toString().trim();
            if (!text.isEmpty()) {
                sections.putIfAbsent(currentSection, text);
            }
        }
    }

}
