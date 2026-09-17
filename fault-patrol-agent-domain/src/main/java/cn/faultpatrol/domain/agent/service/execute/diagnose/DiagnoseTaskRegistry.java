package cn.faultpatrol.domain.agent.service.execute.diagnose;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 诊断任务注册表
 * <p>
 * 维护运行中的诊断会话及其取消标记：
 * - 诊断开始前注册会话，结束后注销
 * - 前端断开 SSE 连接（emitter onCompletion/onError）或调用取消接口时置取消标记
 * - 各阶段节点在每轮执行前检查取消标记，命中则中断诊断链路
 */
@Slf4j
@Component
public class DiagnoseTaskRegistry {

    private final Map<String, AtomicBoolean> cancelledFlags = new ConcurrentHashMap<>();

    /**
     * 注册诊断任务
     */
    public void register(String sessionId) {
        if (sessionId != null) {
            cancelledFlags.put(sessionId, new AtomicBoolean(false));
        }
    }

    /**
     * 注销诊断任务
     */
    public void unregister(String sessionId) {
        if (sessionId != null) {
            cancelledFlags.remove(sessionId);
        }
    }

    /**
     * 取消诊断任务
     *
     * @return 是否成功取消（会话不存在时返回 false）
     */
    public boolean cancel(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        AtomicBoolean flag = cancelledFlags.get(sessionId);
        if (flag == null) {
            return false;
        }
        flag.set(true);
        log.info("诊断任务已请求取消，会话ID：{}", sessionId);
        return true;
    }

    /**
     * 诊断任务是否已被取消
     */
    public boolean isCancelled(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        AtomicBoolean flag = cancelledFlags.get(sessionId);
        return flag != null && flag.get();
    }

    /**
     * 当前运行中的诊断任务数
     */
    public int activeTaskCount() {
        return cancelledFlags.size();
    }

}
