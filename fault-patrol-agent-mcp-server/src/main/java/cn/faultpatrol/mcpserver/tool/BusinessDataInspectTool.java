package cn.faultpatrol.mcpserver.tool;

import cn.faultpatrol.mcpserver.config.InspectToolsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务数据只读巡检工具
 * <p>
 * 通过 JDBC 对业务库执行只读查询（SELECT / SHOW / DESCRIBE / EXPLAIN）：
 * 1. 强制只读：非查询类 SQL 一律拒绝；
 * 2. 表白名单：配置 allowedTables 后仅允许查询白名单表；
 * 3. 行数上限：自动追加 LIMIT 防止大结果集；
 * 4. 执行超时：queryTimeout 控制单次查询时长。
 */
@Slf4j
@Component
public class BusinessDataInspectTool {

    private final InspectToolsProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile JdbcTemplate jdbcTemplate;

    public BusinessDataInspectTool(InspectToolsProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行只读 SQL 查询
     */
    @Tool(description = "对业务数据库执行只读 SQL 查询（仅允许 SELECT/SHOW/DESCRIBE/EXPLAIN），用于故障取证时核对业务数据。返回 JSON 数组结果，最多 maxRows 行")
    public String businessQuery(@ToolParam(description = "只读 SQL 语句，如 SELECT * FROM orders WHERE order_id='O10001'") String sql) {
        if (!properties.getBusinessData().isEnabled()) {
            return toJson(Map.of("error", "业务数据巡检工具未启用"));
        }
        if (sql == null || sql.isBlank()) {
            return toJson(Map.of("error", "SQL 不能为空"));
        }

        // 1. 只读校验
        String normalized = BusinessSqlGuard.stripLeadingComments(sql).trim();
        if (!BusinessSqlGuard.isReadOnlySql(normalized)) {
            return toJson(Map.of("error", "仅允许只读查询（SELECT/SHOW/DESCRIBE/EXPLAIN）"));
        }

        // 2. 表白名单校验
        List<String> allowedTables = properties.getBusinessData().getAllowedTables();
        if (!BusinessSqlGuard.isTableAllowed(BusinessSqlGuard.extractTables(normalized), allowedTables)) {
            return toJson(Map.of("error", "SQL 涉及的表不在允许查询白名单内，白名单: " + allowedTables));
        }

        // 3. 行数上限：无 LIMIT 时自动追加
        int maxRows = properties.getBusinessData().getMaxRows();
        normalized = BusinessSqlGuard.appendLimit(normalized, maxRows);

        // 4. 执行查询
        try {
            final List<Map<String, Object>> rows = new ArrayList<>();
            jdbcTemplate().query(normalized, rs -> {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next() && rows.size() < maxRows + 1) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row.put(metaData.getColumnLabel(i),
                                value == null ? null : truncate(String.valueOf(value), 500));
                    }
                    rows.add(row);
                }
                return null;
            });
            if (rows.size() > maxRows) {
                List<Map<String, Object>> limited = new ArrayList<>(rows.subList(0, maxRows));
                return toJson(Map.of("truncated", true, "count", limited.size(), "rows", limited));
            }
            return toJson(Map.of("count", rows.size(), "rows", rows));
        } catch (Exception e) {
            log.warn("业务数据查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "业务数据查询失败: " + e.getMessage()));
        }
    }

    private JdbcTemplate jdbcTemplate() {
        if (jdbcTemplate == null) {
            synchronized (this) {
                if (jdbcTemplate == null) {
                    InspectToolsProperties.BusinessData businessData = properties.getBusinessData();
                    DriverManagerDataSource dataSource = new DriverManagerDataSource();
                    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    dataSource.setUrl(businessData.getJdbcUrl());
                    dataSource.setUsername(businessData.getUsername());
                    dataSource.setPassword(businessData.getPassword());

                    JdbcTemplate template = new JdbcTemplate(dataSource);
                    template.setQueryTimeout(businessData.getQueryTimeoutSeconds());
                    template.setMaxRows(businessData.getMaxRows());
                    jdbcTemplate = template;
                }
            }
        }
        return jdbcTemplate;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() > maxLength) {
            return value.substring(0, maxLength) + "...(已截断)";
        }
        return value;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }

}
