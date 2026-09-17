package cn.faultpatrol.mcpserver.tool;

import cn.faultpatrol.mcpserver.config.InspectToolsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 容器只读巡检工具
 * <p>
 * 通过 docker CLI 提供容器列表、状态、日志与资源占用等只读查询。
 * 仅执行白名单内的 docker 子命令，不提供任何变更容器的操作。
 */
@Slf4j
@Component
public class ContainerInspectTool {

    private final InspectToolsProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContainerInspectTool(InspectToolsProperties properties) {
        this.properties = properties;
    }

    /**
     * 容器列表
     */
    @Tool(description = "查询容器列表（只读）：容器名、镜像、状态、端口与运行时长")
    public String containerList() {
        if (!properties.getContainer().isEnabled()) {
            return toJson(Map.of("error", "容器巡检工具未启用"));
        }
        return runDocker(List.of("ps", "--format",
                "{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}\t{{.RunningFor}}"), "容器列表查询");
    }

    /**
     * 指定容器状态
     */
    @Tool(description = "查询指定容器状态（只读），包含已停止容器")
    public String containerStatus(@ToolParam(description = "容器名") String name) {
        if (!properties.getContainer().isEnabled()) {
            return toJson(Map.of("error", "容器巡检工具未启用"));
        }
        return runDocker(List.of("ps", "-a", "--filter", "name=" + name, "--format",
                "{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"), "容器状态查询");
    }

    /**
     * 容器日志
     */
    @Tool(description = "查询指定容器最近日志（只读），用于故障取证时查看应用异常")
    public String containerLogs(@ToolParam(description = "容器名") String name,
                                @ToolParam(description = "日志行数，默认 100，最大 500") Integer tailLines) {
        if (!properties.getContainer().isEnabled()) {
            return toJson(Map.of("error", "容器巡检工具未启用"));
        }
        int tail = tailLines == null || tailLines <= 0 ? 100 : Math.min(tailLines, 500);
        return runDocker(List.of("logs", "--tail", String.valueOf(tail), name), "容器日志查询");
    }

    /**
     * 容器资源占用
     */
    @Tool(description = "查询指定容器资源占用（只读）：CPU、内存、网络 IO（单次快照）")
    public String containerStats(@ToolParam(description = "容器名") String name) {
        if (!properties.getContainer().isEnabled()) {
            return toJson(Map.of("error", "容器巡检工具未启用"));
        }
        return runDocker(List.of("stats", "--no-stream", name), "容器资源占用查询");
    }

    /**
     * 执行白名单 docker 命令并收集输出
     */
    private String runDocker(List<String> args, String action) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(properties.getContainer().getDockerCli());
            command.addAll(args);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            boolean finished = process.waitFor(properties.getContainer().getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return toJson(Map.of("error", action + "超时（" + properties.getContainer().getTimeoutSeconds() + "秒）"));
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 1000) {
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            if (process.exitValue() != 0) {
                return toJson(Map.of("error", action + "失败（exit=" + process.exitValue() + "）: " + output));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("action", action);
            result.put("output", output.toString().trim());
            return toJson(result);
        } catch (Exception e) {
            log.warn("{}失败: {}", action, e.getMessage());
            return toJson(Map.of("error", action + "失败: " + e.getMessage()));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }

}
