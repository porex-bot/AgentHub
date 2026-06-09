package com.agenthub.server.config;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AiAccessGuardFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-AgentHub-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String apiToken;
    private final int requestsPerMinute;
    private final ConcurrentMap<String, RequestWindow> windows = new ConcurrentHashMap<>();

    public AiAccessGuardFilter(
            @Value("${agenthub.security.api-token:}") String apiToken,
            @Value("${agenthub.security.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.apiToken = StrUtil.blankToDefault(apiToken, "");
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !isProtectedAiPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!hasValidToken(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid API token");
            return;
        }

        if (isRateLimited(request)) {
            response.sendError(429, "Too many AI requests");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isProtectedAiPath(HttpServletRequest request) {
        String path = StrUtil.blankToDefault(request.getServletPath(), request.getRequestURI());
        return path.equals("/ai") || path.startsWith("/ai/");
    }

    private boolean hasValidToken(HttpServletRequest request) {
        if (StrUtil.isBlank(apiToken)) {
            return true;
        }
        String headerToken = request.getHeader(TOKEN_HEADER);
        if (apiToken.equals(headerToken)) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        return StrUtil.isNotBlank(authorization)
                && authorization.startsWith(BEARER_PREFIX)
                && apiToken.equals(authorization.substring(BEARER_PREFIX.length()));
    }

    private boolean isRateLimited(HttpServletRequest request) {
        if (requestsPerMinute <= 0) {
            return false;
        }

        long minute = Instant.now().getEpochSecond() / 60;
        cleanupOldWindows(minute);
        String key = clientAddress(request) + ":" + minute;
        RequestWindow window = windows.computeIfAbsent(key, ignored -> new RequestWindow(minute));
        return window.count.incrementAndGet() > requestsPerMinute;
    }

    private void cleanupOldWindows(long currentMinute) {
        if (windows.size() < 1000) {
            return;
        }
        Iterator<RequestWindow> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().minute < currentMinute) {
                iterator.remove();
            }
        }
    }

    private static String clientAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return StrUtil.blankToDefault(request.getRemoteAddr(), "unknown");
    }

    private record RequestWindow(long minute, AtomicInteger count) {
        private RequestWindow(long minute) {
            this(minute, new AtomicInteger());
        }
    }
}
