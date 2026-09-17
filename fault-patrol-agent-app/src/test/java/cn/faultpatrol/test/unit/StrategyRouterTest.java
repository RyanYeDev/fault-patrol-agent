package cn.faultpatrol.test.unit;

import cn.faultpatrol.types.design.framework.tree.AbstractStrategyRouter;
import cn.faultpatrol.types.design.framework.tree.StrategyHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 责任链树框架单元测试
 */
public class StrategyRouterTest {

    static class TestNode extends AbstractStrategyRouter<String, StringBuilder, String> {
        private final String name;
        private final StrategyHandler<String, StringBuilder, String> next;

        TestNode(String name, StrategyHandler<String, StringBuilder, String> next) {
            this.name = name;
            this.next = next;
        }

        @Override
        protected String doApply(String request, StringBuilder ctx) throws Exception {
            ctx.append(name);
            return router(request, ctx);
        }

        @Override
        public StrategyHandler<String, StringBuilder, String> get(String request, StringBuilder ctx) {
            return next;
        }
    }

    @Test
    public void testChainRoutesThroughNodes() throws Exception {
        TestNode tail = new TestNode("C", null);
        TestNode mid = new TestNode("B", tail);
        TestNode head = new TestNode("A", mid);

        StringBuilder ctx = new StringBuilder();
        String result = head.apply("request", ctx);

        assertEquals("ABC", ctx.toString());
        assertNull(result);
    }

    @Test
    public void testDefaultHandlerWhenNextIsNull() throws Exception {
        TestNode node = new TestNode("A", null);
        StringBuilder ctx = new StringBuilder();
        // 末端节点 router 时 get 返回 null → 走默认处理器（返回 null）
        String result = node.apply("request", ctx);
        assertEquals("A", ctx.toString());
        assertNull(result);
    }

    @Test
    public void testConditionalRoutingByContext() throws Exception {
        // 根据上下文决定下一跳：首轮进入 B，B 结束后上下文包含 B 则终止
        TestNode nodeB = new TestNode("B", null);
        class ConditionalNode extends AbstractStrategyRouter<String, StringBuilder, String> {
            @Override
            protected String doApply(String request, StringBuilder ctx) throws Exception {
                ctx.append("A");
                return router(request, ctx);
            }

            @Override
            public StrategyHandler<String, StringBuilder, String> get(String request, StringBuilder ctx) {
                return ctx.toString().contains("B") ? null : nodeB;
            }
        }

        StringBuilder ctx = new StringBuilder();
        new ConditionalNode().apply("request", ctx);
        assertEquals("AB", ctx.toString());
    }

}
