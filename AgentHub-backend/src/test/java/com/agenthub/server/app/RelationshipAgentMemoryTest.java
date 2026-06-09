package com.agenthub.server.app;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipAgentMemoryTest {

    @Test
    void saveStreamTurnStoresUserAndAssistantMessages() {
        RecordingChatMemory memory = new RecordingChatMemory();


        assertEquals(2, memory.messages.size());
        assertInstanceOf(UserMessage.class, memory.messages.get(0));
        assertInstanceOf(AssistantMessage.class, memory.messages.get(1));
        assertTrue(((UserMessage) memory.messages.get(0)).getText().contains("小董"));
        assertTrue(((AssistantMessage) memory.messages.get(1)).getText().contains("小董"));
    }

    private static class RecordingChatMemory implements ChatMemory {
        private List<Message> messages = List.of();

        @Override
        public void add(String conversationId, Message message) {
            this.messages = List.of(message);
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public List<Message> get(String conversationId, int lastN) {
            return messages;
        }

        @Override
        public void clear(String conversationId) {
            this.messages = List.of();
        }
    }
}
