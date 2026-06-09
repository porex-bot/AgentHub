package com.agenthub.server.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SseEventFormatter {

    public static final String PREFIX = "__agenthub_event__:";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SseEventFormatter() {
    }

    public static String content(String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "content");
        payload.put("content", content);
        return encode(payload);
    }

    public static String attachment(String label, String url, String fileName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "attachment");
        payload.put("label", label);
        payload.put("url", url);
        payload.put("fileName", fileName);
        payload.put("mimeType", "application/pdf");
        return encode(payload);
    }

    private static String encode(Map<String, Object> payload) {
        try {
            return PREFIX + OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to encode SSE event", e);
        }
    }
}
