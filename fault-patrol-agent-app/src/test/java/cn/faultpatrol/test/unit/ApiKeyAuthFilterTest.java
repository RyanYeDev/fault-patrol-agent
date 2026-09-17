package cn.faultpatrol.test.unit;

import cn.faultpatrol.trigger.config.ApiKeyAuthFilter;
import cn.faultpatrol.trigger.config.SecurityProperties;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * API Key 鉴权过滤器单元测试
 */
public class ApiKeyAuthFilterTest {

    private SecurityProperties properties;
    private ApiKeyAuthFilter filter;

    @Before
    public void setUp() {
        properties = new SecurityProperties();
        properties.setEnabled(true);
        properties.setInspectApiKey("inspect-key");
        properties.setAdminApiKey("admin-key");
        filter = new ApiKeyAuthFilter(properties);
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(path);
        return req;
    }

    @Test
    public void testInspectPathWithCorrectKey() throws ServletException, IOException {
        MockHttpServletRequest req = request("/api/v1/inspect/diagnose");
        req.addHeader("X-Api-Key", "inspect-key");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(200, resp.getStatus());
    }

    @Test
    public void testInspectPathWithWrongKeyReturns401() throws ServletException, IOException {
        MockHttpServletRequest req = request("/api/v1/inspect/diagnose");
        req.addHeader("X-Api-Key", "wrong-key");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(401, resp.getStatus());
        assertTrue(resp.getContentAsString().contains("Unauthorized"));
    }

    @Test
    public void testAdminPathUsesAdminKey() throws ServletException, IOException {
        // inspect key 不能访问 admin 接口
        MockHttpServletRequest req = request("/api/v1/admin/ai-client");
        req.addHeader("X-Api-Key", "inspect-key");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(401, resp.getStatus());

        // admin key 可以访问 admin 接口
        MockHttpServletRequest req2 = request("/api/v1/admin/ai-client");
        req2.addHeader("X-Api-Key", "admin-key");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        filter.doFilter(req2, resp2, new MockFilterChain());
        assertEquals(200, resp2.getStatus());
    }

    @Test
    public void testDisabledFilterPassesThrough() throws ServletException, IOException {
        properties.setEnabled(false);
        MockHttpServletRequest req = request("/api/v1/inspect/diagnose");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(200, resp.getStatus());
    }

    @Test
    public void testEmptyKeyPassesThrough() throws ServletException, IOException {
        properties.setInspectApiKey("");
        MockHttpServletRequest req = request("/api/v1/inspect/diagnose");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(200, resp.getStatus());
    }

    @Test
    public void testNonApiPathSkipped() throws ServletException, IOException {
        MockHttpServletRequest req = request("/index.html");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertEquals(200, resp.getStatus());
    }

}
