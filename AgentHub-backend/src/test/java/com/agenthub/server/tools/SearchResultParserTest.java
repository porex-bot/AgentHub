package com.agenthub.server.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultParserTest {

    @Test
    void parseOrganicResultsReturnsUsefulMessageWhenResultsAreMissing() {
        String result = SearchResultParser.parseOrganicResults("{\"search_metadata\":{\"status\":\"Success\"}}");

        assertEquals("未找到搜索结果。", result);
    }

    @Test
    void parseOrganicResultsLimitsResultsToFiveItems() {
        String result = SearchResultParser.parseOrganicResults("""
                {"organic_results":[
                  {"title":"one"},{"title":"two"},{"title":"three"},
                  {"title":"four"},{"title":"five"},{"title":"six"}
                ]}
                """);

        assertTrue(result.contains("\"title\":\"one\""));
        assertTrue(result.contains("\"title\":\"five\""));
        assertTrue(!result.contains("\"title\":\"six\""));
    }

    @Test
    void parseOrganicResultsFallsBackToResultsArray() {
        String result = SearchResultParser.parseOrganicResults("""
                {"results":[
                  {"title":"Spring AI","link":"https://docs.spring.io/spring-ai/reference/"}
                ]}
                """);

        assertTrue(result.contains("\"title\":\"Spring AI\""));
        assertTrue(result.contains("docs.spring.io"));
    }

    @Test
    void parseOrganicResultsFallsBackToAnswerBox() {
        String result = SearchResultParser.parseOrganicResults("""
                {"answer_box":{"title":"Spring AI","answer":"Spring AI is an application framework for AI engineering."}}
                """);

        assertTrue(result.contains("\"title\":\"Spring AI\""));
        assertTrue(result.contains("application framework"));
    }
}
