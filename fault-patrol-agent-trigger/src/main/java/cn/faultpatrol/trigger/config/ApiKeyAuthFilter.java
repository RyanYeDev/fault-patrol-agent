package cn.faultpatrol.trigger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API Key 鉴权过滤器
 * <p>
 * - /api/v1/admin/**  → 校验 admin-api-key
 * - /api/v1/inspect/** → 校验 inspect-api-key
 * 未配置对应 key 或全局关闭时放行；非 API 路径（首页/静态资源）不受影响。
 */
@Slf4j
@Component
@Order(1)
@EnableConfigurationProperties({SecurityProperties.class, AlertProperties.class})
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final SecurityProperties properties;

    public ApiKeyAuthFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String expectedKey = null;
        if (path.startsWith("/api/v1/admin/")) {
            expectedKey = properties.getAdminApiKey();
        } else if (path.startsWith("/api/v1/inspect/")) {
            expectedKey = properties.getInspectApiKey();
        }

        // 未配置 key 的接口段不鉴权
        if (!StringUtils.hasText(expectedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);
        if (expectedKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("接口鉴权失败：path={}, remote={}", path, request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"0401\",\"info\":\"Unauthorized: 缺少或错误的 API Key\",\"data\":null}");
    }

}
