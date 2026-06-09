package com.agenthub.server.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "spring.ai.mcp.client.enabled=false")
class RelationshipAgentServiceTest {

    @Resource
    private RelationshipAgentService relationshipAgentService;

    @Test
    void chat() {
        String chatId = UUID.randomUUID().toString();
        String message = "我叫小王";
        String answer = relationshipAgentService.chat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void generateRelationshipReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想改善和伴侣的沟通，但不知道怎么开始。";
        RelationshipAgentService.RelationshipReport relationshipReport = relationshipAgentService.generateRelationshipReport(message, chatId);
        Assertions.assertNotNull(relationshipReport);
    }

    @Test
    void chatWithKnowledgeBase() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想改善和伴侣的沟通，但不知道怎么开始。";
        String answer = relationshipAgentService.chatWithKnowledgeBase(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void chatWithMcpTools() {
        String chatId = UUID.randomUUID().toString();
        String message = "请帮我搜索一些适合约会规划的图片。";
        String answer = relationshipAgentService.chatWithMcpTools(message, chatId);
        assertNotNull(answer);
    }
}
