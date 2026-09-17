package cn.faultpatrol.mcpserver.tool;

import cn.faultpatrol.mcpserver.config.InspectToolsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 只读巡检工具
 * <p>
 * 仅提供键扫描、键值查看、TTL、内存与慢日志等只读能力，
 * 不提供任何写入/删除操作，确保巡检过程对业务数据零影响。
 */
@Slf4j
@Component
public class RedisInspectTool {

    private final InspectToolsProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisInspectTool(InspectToolsProperties properties,
                            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 按模式扫描键名
     */
    @Tool(description = "扫描 Redis 键名（只读）。pattern 支持通配符如 'order:*'，返回匹配的键列表，最多 limit 条")
    public String redisScanKeys(@ToolParam(description = "键名模式，如 order:*") String pattern,
                                @ToolParam(description = "最大返回条数，默认 100") Integer limit) {
        if (!properties.getRedis().isEnabled()) {
            return toJson(Map.of("error", "Redis 巡检工具未启用"));
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return toJson(Map.of("error", "Redis 连接未初始化"));
        }
        int max = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        try {
            List<String> keys = new ArrayList<>();
            var connection = redis.getConnectionFactory().getConnection();
            try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(200).build())) {
                while (cursor.hasNext() && keys.size() < max) {
                    keys.add(new String(cursor.next()));
                }
            }
            return toJson(Map.of("pattern", pattern, "count", keys.size(), "keys", keys));
        } catch (Exception e) {
            log.warn("Redis 键扫描失败: {}", e.getMessage());
            return toJson(Map.of("error", "Redis 键扫描失败: " + e.getMessage()));
        }
    }

    /**
     * 查询键的基础信息（类型、TTL、大小）
     */
    @Tool(description = "查询 Redis 键的基础信息（只读）：类型、TTL（秒）、长度或成员数")
    public String redisGetKeyInfo(@ToolParam(description = "键名") String key) {
        if (!properties.getRedis().isEnabled()) {
            return toJson(Map.of("error", "Redis 巡检工具未启用"));
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return toJson(Map.of("error", "Redis 连接未初始化"));
        }
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("key", key);
            info.put("type", redis.type(key) == null ? null : redis.type(key).code());
            info.put("ttlSeconds", redis.getExpire(key, TimeUnit.SECONDS));

            Boolean exists = redis.hasKey(key);
            info.put("exists", Boolean.TRUE.equals(exists));
            if (Boolean.TRUE.equals(exists)) {
                switch (redis.type(key).code()) {
                    case "string" -> {
                        String value = redis.opsForValue().get(key);
                        info.put("stringLength", value == null ? 0 : value.length());
                        info.put("value", truncate(value, 2000));
                    }
                    case "list" -> info.put("listSize", redis.opsForList().size(key));
                    case "hash" -> info.put("hashSize", redis.opsForHash().size(key));
                    case "set" -> info.put("setSize", redis.opsForSet().size(key));
                    case "zset" -> info.put("zsetSize", redis.opsForZSet().size(key));
                    default -> info.put("note", "未知类型");
                }
            }
            return toJson(info);
        } catch (Exception e) {
            log.warn("Redis 键信息查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Redis 键信息查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询字符串类型键的值
     */
    @Tool(description = "查询 Redis 字符串类型键的值（只读），超长自动截断")
    public String redisGetString(@ToolParam(description = "键名") String key) {
        if (!properties.getRedis().isEnabled()) {
            return toJson(Map.of("error", "Redis 巡检工具未启用"));
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return toJson(Map.of("error", "Redis 连接未初始化"));
        }
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                return toJson(Map.of("key", key, "exists", false));
            }
            return toJson(Map.of("key", key, "value", truncate(value, 4000)));
        } catch (Exception e) {
            log.warn("Redis 键值查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Redis 键值查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询内存信息
     */
    @Tool(description = "查询 Redis 内存使用信息（只读）：used_memory、峰值、内存碎片率等")
    public String redisMemoryInfo() {
        if (!properties.getRedis().isEnabled()) {
            return toJson(Map.of("error", "Redis 巡检工具未启用"));
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return toJson(Map.of("error", "Redis 连接未初始化"));
        }
        try {
            var connection = redis.getConnectionFactory().getConnection();
            var memory = connection.info("memory");
            Map<String, Object> result = new HashMap<>();
            result.put("used_memory", memory.get("used_memory"));
            result.put("used_memory_human", memory.get("used_memory_human"));
            result.put("used_memory_peak_human", memory.get("used_memory_peak_human"));
            result.put("mem_fragmentation_ratio", memory.get("mem_fragmentation_ratio"));
            result.put("maxmemory_human", memory.get("maxmemory_human"));
            return toJson(result);
        } catch (Exception e) {
            log.warn("Redis 内存信息查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Redis 内存信息查询失败: " + e.getMessage()));
        }
    }

    /**
     * 查询慢日志
     */
    @Tool(description = "查询 Redis 慢日志（只读），返回最近 count 条慢命令")
    public String redisSlowLog(@ToolParam(description = "条数，默认 10") Integer count) {
        if (!properties.getRedis().isEnabled()) {
            return toJson(Map.of("error", "Redis 巡检工具未启用"));
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return toJson(Map.of("error", "Redis 连接未初始化"));
        }
        int size = count == null || count <= 0 ? 10 : Math.min(count, 50);
        try {
            // spring-data-redis 3.4 移除了 slowLog API，通过 Lettuce 原生连接查询
            var lettuceFactory = (org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory) redis.getConnectionFactory();
            io.lettuce.core.RedisClient redisClient = (io.lettuce.core.RedisClient) lettuceFactory.getNativeClient();
            List<Object> slowlogs;
            try (var nativeConnection = redisClient.connect()) {
                slowlogs = nativeConnection.sync().slowlogGet(size);
            }
            List<Map<String, Object>> logs = new ArrayList<>();
            for (Object slowlog : slowlogs) {
                if (slowlog instanceof List<?> entry && entry.size() >= 4) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", entry.get(0));
                    item.put("timestamp", entry.get(1));
                    item.put("costMicros", entry.get(2));
                    List<?> args = (List<?>) entry.get(3);
                    item.put("command", args.stream().map(String::valueOf).reduce((a, b) -> a + " " + b).orElse(""));
                    logs.add(item);
                }
            }
            return toJson(Map.of("count", logs.size(), "slowlogs", logs));
        } catch (Exception e) {
            log.warn("Redis 慢日志查询失败: {}", e.getMessage());
            return toJson(Map.of("error", "Redis 慢日志查询失败: " + e.getMessage()));
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
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
