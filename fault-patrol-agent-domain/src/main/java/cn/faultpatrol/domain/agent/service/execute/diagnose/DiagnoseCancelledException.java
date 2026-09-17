package cn.faultpatrol.domain.agent.service.execute.diagnose;

/**
 * 诊断任务被取消异常：由调度服务捕获后以 SSE 消息回传「诊断已取消」
 */
public class DiagnoseCancelledException extends RuntimeException {

    public DiagnoseCancelledException() {
        super("诊断任务已取消");
    }

}
