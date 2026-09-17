package cn.faultpatrol.types.design.framework.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 策略路由抽象类
 * <p>
 * 责任链树的骨架实现：{@link #apply(Object, Object)} 受理请求并执行当前节点业务逻辑
 * （{@link #doApply(Object, Object)}），节点内部通过 {@link #router(Object, Object)}
 * 结合 {@link #get(Object, Object)} 动态选择并驱动下一个节点，从而支持有条件的
 * 链路流转与回环（例如质量监督不通过时回退到规划节点重新执行）。
 *
 * @param <T> 入参类型
 * @param <D> 动态上下文类型
 * @param <R> 返参类型
 */
public abstract class AbstractStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    private final Logger log = LoggerFactory.getLogger(AbstractStrategyRouter.class);

    /**
     * 缺省策略处理器，链路末端兜底
     */
    protected StrategyHandler<T, D, R> defaultStrategyHandler = StrategyHandler.DEFAULT;

    public void setDefaultStrategyHandler(StrategyHandler<T, D, R> defaultStrategyHandler) {
        this.defaultStrategyHandler = defaultStrategyHandler;
    }

    /**
     * 路由到下一个节点并驱动其执行
     *
     * @param requestParameter 入参
     * @param dynamicContext   动态上下文
     * @return 处理结果
     * @throws Exception 处理异常
     */
    public R router(T requestParameter, D dynamicContext) throws Exception {
        StrategyHandler<T, D, R> strategyHandler = get(requestParameter, dynamicContext);
        if (null != strategyHandler) {
            log.debug("责任链路由 -> {}", strategyHandler.getClass().getSimpleName());
            return strategyHandler.apply(requestParameter, dynamicContext);
        }
        return defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }

    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        return doApply(requestParameter, dynamicContext);
    }

    /**
     * 当前节点业务逻辑
     *
     * @param requestParameter 入参
     * @param dynamicContext   动态上下文
     * @return 处理结果
     * @throws Exception 处理异常
     */
    protected abstract R doApply(T requestParameter, D dynamicContext) throws Exception;

}
