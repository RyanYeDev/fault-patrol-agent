package cn.faultpatrol.mcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检工具配置属性
 * <p>
 * 每个巡检工具均带 enabled 开关与超时配置；未启用或目标服务不可达时，
 * 工具调用会返回明确的不可用信息，不影响其他工具。
 */
@Data
@ConfigurationProperties(prefix = "faultpatrol.tools")
public class InspectToolsProperties {

    /**
     * Redis 巡检工具配置
     */
    private Redis redis = new Redis();

    /**
     * RabbitMQ 巡检工具配置（管理 HTTP API）
     */
    private Rabbitmq rabbitmq = new Rabbitmq();

    /**
     * Prometheus 巡检工具配置
     */
    private Prometheus prometheus = new Prometheus();

    /**
     * Jaeger 巡检工具配置
     */
    private Jaeger jaeger = new Jaeger();

    /**
     * 容器巡检工具配置（docker CLI）
     */
    private Container container = new Container();

    /**
     * 业务数据巡检工具配置（只读 SQL）
     */
    private BusinessData businessData = new BusinessData();

    @Data
    public static class Redis {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        /**
         * 命令超时（毫秒）
         */
        private long timeoutMs = 3000;
    }

    @Data
    public static class Rabbitmq {
        private boolean enabled = false;
        /**
         * 管理 API 地址，如 http://localhost:15672
         */
        private String managementUrl = "http://localhost:15672";
        private String username = "guest";
        private String password = "guest";
        /**
         * HTTP 超时
         */
        private Duration timeout = Duration.ofSeconds(5);
    }

    @Data
    public static class Prometheus {
        private boolean enabled = false;
        /**
         * Prometheus 地址，如 http://localhost:9090
         */
        private String baseUrl = "http://localhost:9090";
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Jaeger {
        private boolean enabled = false;
        /**
         * Jaeger Query 地址，如 http://localhost:16686
         */
        private String baseUrl = "http://localhost:16686";
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Container {
        private boolean enabled = false;
        /**
         * docker CLI 路径（默认从 PATH 解析）
         */
        private String dockerCli = "docker";
        /**
         * 命令执行超时（秒）
         */
        private long timeoutSeconds = 30;
    }

    @Data
    public static class BusinessData {
        private boolean enabled = false;
        private String jdbcUrl = "jdbc:mysql://localhost:3306/fault_patrol_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        private String username = "root";
        private String password = "";
        /**
         * 允许查询的表白名单（空表示仅允许 SHOW / DESCRIBE 等元信息查询）
         */
        private List<String> allowedTables = new ArrayList<>();
        /**
         * 最大返回行数
         */
        private int maxRows = 200;
        /**
         * SQL 执行超时（秒）
         */
        private int queryTimeoutSeconds = 10;
    }

}
