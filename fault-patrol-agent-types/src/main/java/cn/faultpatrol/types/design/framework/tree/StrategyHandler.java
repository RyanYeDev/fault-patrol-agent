package cn.faultpatrol.types.design.framework.tree;

/**
 * 策略处理器接口
 * <p>
 * 责任链树中的节点抽象。每个节点负责处理一部分业务逻辑，
 * 并在处理完成后通过 {@link StrategyMapper#get(Object, Object)} 决定下一个节点，
 * 由 {@link AbstractStrategyRouter#router(Object, Object)} 完成节点间的流转。
 *
 * @param <T> 入参类型
 * @param <D> 动态上下文类型
 * @param <R> 返参类型
 */
public interface StrategyHandler<T, D, R> {

    /**
     * 缺省处理器，直接返回 null 用于终止链路
     */
    StrategyHandler DEFAULT = (T, D) -> null;

    /**
     * 受理请求
     *
     * @param requestParameter 入参
     * @param dynamicContext   动态上下文
     * @return 处理结果
     * @throws Exception 处理异常
     */
    R apply(T requestParameter, D dynamicContext) throws Exception;

}
