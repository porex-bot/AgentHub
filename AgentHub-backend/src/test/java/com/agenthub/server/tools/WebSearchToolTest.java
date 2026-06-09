package com.agenthub.server.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSearchToolTest {

    @Test
    void searchWeb() {
        WebSearchTool webSearchTool = new WebSearchTool("test-api-key");
        String query = "claudecode";
        String result = webSearchTool.searchWeb(query);
        assertNotNull(result);

    }
}

