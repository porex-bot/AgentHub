package com.agenthub.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.ai.mcp.client.enabled=false")
class AgentApplicationTests {

    @Test
    void contextLoads() {
    }
}


