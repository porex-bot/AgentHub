package com.agenthub.server.tools;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class WebSearchToolOkHttp {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;
    private final OkHttpClient client;
    private final String searchApiUrl;

    public WebSearchToolOkHttp(String apiKey) {
        this(apiKey, new OkHttpClient(), SEARCH_API_URL);
    }

    WebSearchToolOkHttp(String apiKey, OkHttpClient client, String searchApiUrl) {
        this.apiKey = apiKey;
        this.client = client;
        this.searchApiUrl = searchApiUrl;
    }

    @Tool(description = "Search for information from Bing Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        try {
            HttpUrl url = HttpUrl.get(searchApiUrl).newBuilder()
                    .addQueryParameter("engine", "bing")
                    .addQueryParameter("q", query)
                    .addQueryParameter("api_key", apiKey)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return "未找到搜索结果。";
                }
                String responseText = responseBody.string();
                if (!response.isSuccessful()) {
                    log.warn("Search API request failed for query '{}', httpStatus={}, message={}, bodyLength={}",
                            query,
                            response.code(),
                            response.message(),
                            responseText.length());
                    return "搜索服务暂时不可用（HTTP " + response.code() + " " + response.message()
                            + "），请稍后重试或检查搜索 API 配额。";
                }
                String result = SearchResultParser.parseOrganicResults(responseText);
                if ("未找到搜索结果。".equals(result)) {
                    log.warn("Search API returned no parsed results for query '{}', httpStatus={}, bodyLength={}",
                            query,
                            response.code(),
                            responseText.length());
                }
                return result;
            }
        } catch (Exception e) {
            return "Error searching Bing: " + e.getMessage();
        }
    }
}


