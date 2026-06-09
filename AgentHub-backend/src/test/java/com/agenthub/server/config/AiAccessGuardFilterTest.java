package com.agenthub.server.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiAccessGuardFilterTest {

    @Test
    void rejectsAiRequestsWithoutConfiguredToken() throws ServletException, IOException {
        AiAccessGuardFilter filter = new AiAccessGuardFilter("secret-token", 60);
        MockHttpServletRequest request = aiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void allowsAiRequestsWithBearerToken() throws ServletException, IOException {
        AiAccessGuardFilter filter = new AiAccessGuardFilter("secret-token", 60);
        MockHttpServletRequest request = aiRequest();
        request.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void rateLimitsAiRequestsByClientAddress() throws ServletException, IOException {
        AiAccessGuardFilter filter = new AiAccessGuardFilter("", 1);
        MockFilterChain firstChain = new MockFilterChain();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(aiRequest(), firstResponse, firstChain);

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(aiRequest(), secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void ignoresNonAiRequests() throws ServletException, IOException {
        AiAccessGuardFilter filter = new AiAccessGuardFilter("secret-token", 1);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.setServletPath("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private static MockHttpServletRequest aiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ai/agents/task/chat/stream");
        request.setServletPath("/ai/agents/task/chat/stream");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
