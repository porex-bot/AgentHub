package com.agenthub.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiControllerMappingTest {

    @Test
    void compatibilityTaskChatProducesTextEventStream() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("streamCompatibilityTaskAgentChat", String.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertArrayEquals(new String[]{"/task/chat"}, mapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
    }

    @Test
    void compatibilityReportChatProducesTextEventStream() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("streamCompatibilityResearchReportChat", String.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
    }

    @Test
    void agentHubRelationshipChatUsesStableStreamPath() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("streamRelationshipChat", String.class, String.class);
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/agents/relationship/chat/stream"}, mapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
    }

    @Test
    void agentHubTaskChatUsesStableStreamPath() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("streamTaskAgentChat", String.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/agents/task/chat/stream"}, mapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
    }

    @Test
    void agentHubResearchChatUsesStableStreamPath() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("streamResearchReportChat", String.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/agents/research/chat/stream"}, mapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE}, mapping.produces());
    }

    @Test
    void pdfDownloadUsesStablePath() throws NoSuchMethodException {
        Method method = AiController.class.getDeclaredMethod("downloadPdf", String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/files/pdf/{fileName:.+}"}, mapping.value());
    }
}


