package com.agenthub.server.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.stream.Collectors;

final class SearchResultParser {

    private static final int MAX_RESULTS = 5;

    private SearchResultParser() {
    }

    static String parseOrganicResults(String responseBody) {
        JSONObject jsonObject = JSONUtil.parseObj(responseBody);
        JSONArray results = firstNonEmptyArray(jsonObject, "organic_results", "results", "items");
        if (results != null) {
            return formatResultArray(results);
        }

        JSONObject answerBox = jsonObject.getJSONObject("answer_box");
        if (answerBox != null && !answerBox.isEmpty()) {
            return answerBox.toString();
        }

        JSONObject knowledgeGraph = jsonObject.getJSONObject("knowledge_graph");
        if (knowledgeGraph != null && !knowledgeGraph.isEmpty()) {
            return knowledgeGraph.toString();
        }

        return "未找到搜索结果。";
    }

    private static JSONArray firstNonEmptyArray(JSONObject jsonObject, String... keys) {
        for (String key : keys) {
            JSONArray array = jsonObject.getJSONArray(key);
            if (array != null && !array.isEmpty()) {
                return array;
            }
        }
        return null;
    }

    private static String formatResultArray(JSONArray results) {
        int resultCount = Math.min(MAX_RESULTS, results.size());
        List<Object> objects = results.subList(0, resultCount);
        return objects.stream()
                .map(obj -> ((JSONObject) obj).toString())
                .collect(Collectors.joining(","));
    }
}
