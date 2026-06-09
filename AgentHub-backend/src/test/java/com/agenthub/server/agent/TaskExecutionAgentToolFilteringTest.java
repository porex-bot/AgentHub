package com.agenthub.server.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskExecutionAgentToolFilteringTest {

    @Test
    void filterInternalToolsRemovesTerminateTool() {
        ToolCallback searchWeb = tool("searchWeb");
        ToolCallback terminate = tool("doTerminate");

        ToolCallback[] result = TaskExecutionAgent.filterInternalTools(new ToolCallback[]{searchWeb, terminate});

        assertArrayEquals(new ToolCallback[]{searchWeb}, result);
    }

    private static ToolCallback tool(String name) {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);

        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }
}


