package cn.faultpatrol.mcpserver.tool;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 业务库只读 SQL 守卫单元测试
 */
public class BusinessSqlGuardTest {

    @Test
    public void testReadOnlyDetection() {
        assertTrue(BusinessSqlGuard.isReadOnlySql("SELECT * FROM orders"));
        assertTrue(BusinessSqlGuard.isReadOnlySql("  show tables"));
        assertTrue(BusinessSqlGuard.isReadOnlySql("DESCRIBE orders"));
        assertTrue(BusinessSqlGuard.isReadOnlySql("explain select 1"));
        assertFalse(BusinessSqlGuard.isReadOnlySql("UPDATE orders SET status='X'"));
        assertFalse(BusinessSqlGuard.isReadOnlySql("DELETE FROM orders"));
        assertFalse(BusinessSqlGuard.isReadOnlySql("INSERT INTO orders VALUES (1)"));
        assertFalse(BusinessSqlGuard.isReadOnlySql("DROP TABLE orders"));
    }

    @Test
    public void testStripLeadingComments() {
        String sql = "-- 查询订单\n/* 注释块 */ SELECT * FROM orders";
        String stripped = BusinessSqlGuard.stripLeadingComments(sql);
        assertTrue(stripped.trim().startsWith("SELECT"));
    }

    @Test
    public void testExtractTables() {
        List<String> tables = BusinessSqlGuard.extractTables(
                "SELECT o.id, i.stock FROM orders o JOIN inventory i ON o.sku_id = i.sku_id");
        assertEquals(2, tables.size());
        assertTrue(tables.contains("orders"));
        assertTrue(tables.contains("inventory"));
    }

    @Test
    public void testExtractTablesBacktick() {
        List<String> tables = BusinessSqlGuard.extractTables("SELECT * FROM `orders`");
        assertEquals(1, tables.size());
        assertEquals("orders", tables.get(0));
    }

    @Test
    public void testWhitelistEmptyAllowsAll() {
        assertTrue(BusinessSqlGuard.isTableAllowed(List.of("orders"), List.of()));
        assertTrue(BusinessSqlGuard.isTableAllowed(List.of("orders"), null));
    }

    @Test
    public void testWhitelistEnforced() {
        List<String> allowed = List.of("orders", "inventory");
        assertTrue(BusinessSqlGuard.isTableAllowed(List.of("orders"), allowed));
        assertTrue(BusinessSqlGuard.isTableAllowed(List.of("users", "orders"), allowed));
        assertFalse(BusinessSqlGuard.isTableAllowed(List.of("payment_flow"), allowed));
        assertFalse(BusinessSqlGuard.isTableAllowed(List.of(), allowed));
    }

    @Test
    public void testAppendLimit() {
        assertEquals("SELECT * FROM orders LIMIT 200",
                BusinessSqlGuard.appendLimit("SELECT * FROM orders", 200));
        assertEquals("SELECT * FROM orders; LIMIT 200".replace("; ", " "),
                BusinessSqlGuard.appendLimit("SELECT * FROM orders;", 200));
        // 已有 LIMIT 不重复追加
        assertEquals("SELECT * FROM orders LIMIT 10",
                BusinessSqlGuard.appendLimit("SELECT * FROM orders LIMIT 10", 200));
        // 非 SELECT 不追加
        assertEquals("SHOW TABLES", BusinessSqlGuard.appendLimit("SHOW TABLES", 200));
    }

}
