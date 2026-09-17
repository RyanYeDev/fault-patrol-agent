package cn.faultpatrol.types.design.framework.tree;

/**
 * 策略映射器接口
 * <p>
 * 由链路节点实现，根据入参与上下文动态决定下一个待执行的策略节点。
 *
 * @param <T> 入参类型
 * @param <D> 动态上下文类型
 * @param <R> 返参类型
 */
public interface StrategyMapper<T, D, R> {

    /**
     * 获取待执行策略
     *
     * @param requestParameter 入参
     * @param dynamicContext   动态上下文
     * @return 下一个策略节点；返回 null 时由缺省处理器兜底
     * @throws Exception 异常
     */
    StrategyHandler<T, D, R> get(T requestParameter, D dynamicContext) throws Exception;

}
