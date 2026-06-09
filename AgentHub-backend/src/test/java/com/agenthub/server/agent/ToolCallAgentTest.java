package com.agenthub.server.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class ToolCallAgentTest {

    @Test
    void formatToolResultsHidesRawResponseData() {
        TestableToolCallAgent agent = new TestableToolCallAgent();
        ToolResponseMessage message = new ToolResponseMessage(List.of(
                new ToolResponseMessage.ToolResponse("1", "searchWeb", "{\"title\":\"raw result\"}")
        ));

        String result = agent.formatForDisplay(message);

        assertEquals("已调用工具：searchWeb，正在整理结果。", result);
        assertFalse(result.contains("{\"title\""));
    }

    @Test
    void toolProgressIsNotPublishedToUser() {
        TestableToolCallAgent agent = new TestableToolCallAgent();

        assertFalse(agent.publish("已调用工具：searchWeb，正在整理结果。"));
    }

    @Test
    void pseudoSearchTagIsExecutedAndNotPublished() {
        ToolCallback searchWeb = tool("searchWeb", "搜索结果：Agent 趋势资料");
        TestableToolCallAgent agent = new TestableToolCallAgent(searchWeb);
        String pseudoToolResponse = """
                <search>
                {"name": "searchWeb", "arguments": {"query": "2026agent大趋势"}}
                </search>
                """;

        boolean handled = agent.handlePseudoTool(pseudoToolResponse);

        assertTrue(handled);
        verify(searchWeb).call("{\"query\":\"2026agent大趋势\"}");
        assertFalse(agent.publish(pseudoToolResponse));
        assertTrue(agent.messages().stream()
                .map(Message::getText)
                .anyMatch(text -> text.contains("工具 searchWeb 返回") && text.contains("搜索结果：Agent 趋势资料")));
        assertFalse(agent.messages().stream()
                .map(Message::getText)
                .anyMatch(text -> text.contains("<search>")));
    }

    @Test
    void promisedSearchIsExecutedAndNotPublished() {
        ToolCallback searchWeb = tool("searchWeb", "{\"title\":\"Spring AI Reference\"}");
        TestableToolCallAgent agent = new TestableToolCallAgent(searchWeb);
        String promisedSearch = "现在，我开始执行下一步：搜索 \"Spring AI features\" 的相关信息。";

        boolean handled = agent.handlePromisedTool(promisedSearch);

        assertTrue(handled);
        verify(searchWeb).call("{\"query\":\"Spring AI features\"}");
        assertFalse(agent.publish(promisedSearch));
        assertTrue(agent.messages().stream()
                .map(Message::getText)
                .anyMatch(text -> text.contains("工具 searchWeb 返回") && text.contains("Spring AI Reference")));
        assertFalse(agent.messages().stream()
                .map(Message::getText)
                .anyMatch(text -> text.contains("现在，我开始执行下一步")));
    }

    @Test
    void promisedSearchWithKeywordBeforeSearchVerbIsExecuted() {
        ToolCallback searchWeb = tool("searchWeb", "{\"title\":\"Agent Trends\"}");
        TestableToolCallAgent agent = new TestableToolCallAgent(searchWeb);
        String promisedSearch = """
                我将调用 `searchWeb` 工具，使用关键词“2026 Agent trends”进行搜索，以获取相关资料。
                """;

        boolean handled = agent.handlePromisedTool(promisedSearch);

        assertTrue(handled);
        verify(searchWeb).call("{\"query\":\"2026 Agent trends\"}");
        assertFalse(agent.publish(promisedSearch));
        assertTrue(agent.messages().stream()
                .map(Message::getText)
                .anyMatch(text -> text.contains("工具 searchWeb 返回") && text.contains("Agent Trends")));
    }

    @Test
    void pseudoToolTagsAreRemovedFromFinalResponse() {
        TestableToolCallAgent agent = new TestableToolCallAgent();

        String result = agent.normalize("""
                <search>
                {"name": "searchWeb", "arguments": {"query": "财务报告"}}
                </search>
                """);

        assertEquals("正在调用工具，请稍后重试。", result);
    }

    private static class TestableToolCallAgent extends ToolCallAgent {

        private TestableToolCallAgent() {
            super(new ToolCallback[0]);
        }

        private TestableToolCallAgent(ToolCallback... tools) {
            super(tools);
        }

        @Override
        protected Set<String> getPseudoToolCallFallbackToolNames() {
            return Set.of("searchWeb");
        }

        @Override
        protected Set<String> getPromisedToolCallFallbackToolNames() {
            return Set.of("searchWeb");
        }

        private String formatForDisplay(ToolResponseMessage message) {
            return formatToolResultsForDisplay(message);
        }

        private boolean publish(String result) {
            return shouldPublishStepResult(result);
        }

        private boolean handlePseudoTool(String response) {
            return handlePseudoToolCallIfPresent(response);
        }

        private boolean handlePromisedTool(String response) {
            return handlePromisedToolCallIfPresent(response);
        }

        private List<Message> messages() {
            return getMessageList();
        }

        private String normalize(String response) {
            return normalizeFinalResponse(response);
        }
    }

    private static ToolCallback tool(String name, String response) {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);

        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(callback.call(anyString())).thenReturn(response);
        return callback;
    }
}
