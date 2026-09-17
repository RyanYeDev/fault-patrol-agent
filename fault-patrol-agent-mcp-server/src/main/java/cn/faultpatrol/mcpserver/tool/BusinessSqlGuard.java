package cn.faultpatrol.mcpserver.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务库只读 SQL 守卫
 * <p>
 * 校验规则：
 * 1. 强制只读：仅允许 SELECT / SHOW / DESCRIBE / DESC / EXPLAIN
 * 2. 表白名单：配置 allowedTables 后仅允许查询白名单表
 * 3. 行数上限：SELECT 语句自动追加 LIMIT
 */
public final class BusinessSqlGuard {

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:from|join|update|into|table)\\s+([`\"]?[a-zA-Z0-9_]+[`\"]?)");

    private BusinessSqlGuard() {
    }

    /**
     * 去除 SQL 前导注释
     */
    public static String stripLeadingComments(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("(?s)^(\\s*(?:--[^\\n]*\\n|/\\*.*?\\*/\\s*))+", "");
    }

    /**
     * 是否为只读 SQL
     */
    public static boolean isReadOnlySql(String sql) {
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT") || upper.startsWith("SHOW")
                || upper.startsWith("DESCRIBE") || upper.startsWith("DESC")
                || upper.startsWith("EXPLAIN");
    }

    /**
     * 提取 SQL 中涉及的表名
     */
    public static List<String> extractTables(String sql) {
        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_PATTERN.matcher(sql == null ? "" : sql);
        while (matcher.find()) {
            String table = matcher.group(1).replaceAll("[`\"]", "");
            if (!tables.contains(table)) {
                tables.add(table);
            }
        }
        return tables;
    }

    /**
     * 校验表白名单：白名单为空时放行；否则要求至少命中一个白名单表
     */
    public static boolean isTableAllowed(List<String> extractedTables, List<String> allowedTables) {
        if (allowedTables == null || allowedTables.isEmpty()) {
            return true;
        }
        for (String table : extractedTables) {
            for (String allowed : allowedTables) {
                if (allowed != null && allowed.equalsIgnoreCase(table)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * SELECT 语句自动追加 LIMIT（已含 LIMIT 时不重复追加）
     */
    public static String appendLimit(String sql, int maxRows) {
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("SELECT") && !upper.contains("LIMIT")) {
            return sql.replaceAll(";\\s*$", "") + " LIMIT " + maxRows;
        }
        return sql;
    }

}
