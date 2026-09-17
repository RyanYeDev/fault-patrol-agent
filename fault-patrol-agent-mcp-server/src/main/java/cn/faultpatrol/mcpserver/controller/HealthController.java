package cn.faultpatrol.mcpserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查接口：供 docker compose 健康探测使用
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "fault-patrol-agent-mcp-server"
        );
    }

}
