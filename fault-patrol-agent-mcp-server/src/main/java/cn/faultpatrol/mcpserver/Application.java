package cn.faultpatrol.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 巡检工具 MCP 服务器启动类
 * <p>
 * 以 SSE 传输对外暴露只读巡检工具（业务数据 / Redis / MQ / 容器 / Prometheus / Jaeger），
 * 供故障巡检 Agent 通过 MCP 协议调用完成交叉取证。
 */
@SpringBootApplication
@EnableConfigurationProperties(cn.faultpatrol.mcpserver.config.InspectToolsProperties.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
