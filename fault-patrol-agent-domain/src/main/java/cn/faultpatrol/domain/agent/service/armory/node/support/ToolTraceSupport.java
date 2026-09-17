package cn.faultpatrol.domain.agent.service.armory.node.support;

import com.alibaba.fastjson.JSON;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用轨迹采集器
 * <p>
 * 通过装饰 ToolCallback 在工具执行时记录（工具名 / 入参 / 结果 / 耗时 / 错误），
 * 记录写入线程本地存储——MCP 工具由调用线程同步执行，因此阶段节点可在
 * LLM 调用完成后通过 {@link #drain()} 取走本轮全部工具调用轨迹。
 */
public final class ToolTraceSupport {

    /** 单条记录字段长度上限，防止超长工具结果撑爆存储 */
    private static final int MAX_FIELD_LENGTH = 4000;

    private static final ThreadLocal<List<Map<String, Object>>> TRACE =
            ThreadLocal.withInitial(ArrayList::new);

    private ToolTraceSupport() {
    }

    /**
     * 记录一次工具调用
     */
    public static void record(String toolName, String input, String result, long costMs, String error) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tool", toolName);
        item.put("input", truncate(input));
        item.put("result", truncate(result));
        item.put("costMs", costMs);
        item.put("error", error);
        item.put("time", System.currentTimeMillis());
        TRACE.get().add(item);
    }

    /**
     * 取走并清空当前线程累积的工具调用轨迹
     */
    public static List<Map<String, Object>> drain() {
        List<Map<String, Object>> snapshot = new ArrayList<>(TRACE.get());
        TRACE.get().clear();
        return snapshot;
    }

    /**
     * 包装 ToolCallbackProvider：为每个回调附加轨迹记录
     */
    public static ToolCallbackProvider wrap(ToolCallbackProvider provider) {
        ToolCallback[] callbacks = provider.getToolCallbacks();
        ToolCallback[] wrapped = new ToolCallback[callbacks.length];
        for (int i = 0; i < callbacks.length; i++) {
            wrapped[i] = decorate(callbacks[i]);
        }
        return () -> wrapped;
    }

    private static ToolCallback decorate(ToolCallback delegate) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                long start = System.currentTimeMillis();
                try {
                    String result = delegate.call(toolInput);
                    record(delegate.getToolDefinition().name(), toolInput, result,
                            System.currentTimeMillis() - start, null);
                    return result;
                } catch (Exception e) {
                    record(delegate.getToolDefinition().name(), toolInput, null,
                            System.currentTimeMillis() - start, e.getMessage());
                    throw e;
                }
            }

            @Override
            public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
                long start = System.currentTimeMillis();
                try {
                    String result = delegate.call(toolInput, toolContext);
                    record(delegate.getToolDefinition().name(), toolInput, result,
                            System.currentTimeMillis() - start, null);
                    return result;
                } catch (Exception e) {
                    record(delegate.getToolDefinition().name(), toolInput, null,
                            System.currentTimeMillis() - start, e.getMessage());
                    throw e;
                }
            }
        };
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() > MAX_FIELD_LENGTH) {
            return value.substring(0, MAX_FIELD_LENGTH) + "...(已截断)";
        }
        return value;
    }

}
