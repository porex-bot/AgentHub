package com.agenthub.server.tools;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSearchToolOkHttpTest {

    @Test
    void searchWebReturnsUsefulMessageWhenBingHasNoOrganicResults() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(
                                "{\"search_metadata\":{\"status\":\"Success\"}}",
                                MediaType.get("application/json")
                        ))
                        .build())
                .build();
        WebSearchToolOkHttp webSearchTool = new WebSearchToolOkHttp(
                "test-api-key",
                client,
                "https://example.test/search"
        );

        String result = webSearchTool.searchWeb("2026 AI agent trends");

        assertEquals("未找到搜索结果。", result);
    }

    @Test
    void searchWebReportsHttpFailuresInsteadOfEmptyResults() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(429)
                        .message("Too Many Requests")
                        .body(ResponseBody.create(
                                "{\"error\":\"rate limit\"}",
                                MediaType.get("application/json")
                        ))
                        .build())
                .build();
        WebSearchToolOkHttp webSearchTool = new WebSearchToolOkHttp(
                "test-api-key",
                client,
                "https://example.test/search"
        );

        String result = webSearchTool.searchWeb("2026 AI agent trends");

        assertEquals("搜索服务暂时不可用（HTTP 429 Too Many Requests），请稍后重试或检查搜索 API 配额。", result);
    }
}
