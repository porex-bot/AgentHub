package com.agenthub.server.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TaskExecutionAgentTest {

    @Test
    void configuresPdfOnlyDocumentGenerationPrompt() {
        TaskExecutionAgent agent = new TaskExecutionAgent(new ToolCallback[0], mock(ChatModel.class));

        assertEquals("TaskExecutionAgent", agent.getName());
        assertTrue(agent.getSystemPrompt().contains("仅支持 PDF"));
        assertTrue(agent.getSystemPrompt().contains("Word、Excel、PPT、Markdown、TXT、DOCX"));
        assertTrue(agent.getNextStepPrompt().contains("非 PDF 文档生成请求"));
        assertTrue(agent.getPromisedToolCallFallbackToolNames().contains("searchWeb"));
    }
}
