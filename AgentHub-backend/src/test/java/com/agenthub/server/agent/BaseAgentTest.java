package com.agenthub.server.agent;

import com.agenthub.server.agent.model.AgentState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseAgentTest {

    @Test
    void runReturnsOnlyPublishableResultsWithoutStepPrefixes() {
        PublishableResultAgent agent = new PublishableResultAgent();

        String result = agent.run("find attractions");

        assertEquals("final answer", result);
    }

    @Test
    void runLoadsHistoryAndSavesOnlyUserAndFinalAnswer() {
        RecordingChatMemory memory = new RecordingChatMemory();
        memory.history.add(new UserMessage("previous user message"));
        memory.history.add(new AssistantMessage("previous assistant message"));

        MemoryAwareAgent agent = new MemoryAwareAgent();
        agent.setChatMemory(memory);
        agent.setConversationId("task-chat-1");

        String result = agent.run("current task");

        assertEquals("history size: 3", result);
        assertEquals("task-chat-1", memory.savedConversationId);
        assertEquals(2, memory.savedMessages.size());
        assertEquals("current task", ((UserMessage) memory.savedMessages.get(0)).getText());
        assertEquals("history size: 3", ((AssistantMessage) memory.savedMessages.get(1)).getText());
    }

    @Test
    void getLastAssistantMemoryTextReturnsMostRecentAssistantMessage() {
        RecordingChatMemory memory = new RecordingChatMemory();
        memory.history.add(new UserMessage("first request"));
        memory.history.add(new AssistantMessage("first answer"));
        memory.history.add(new UserMessage("second request"));
        memory.history.add(new AssistantMessage("second answer"));

        MemoryAwareAgent agent = new MemoryAwareAgent();
        agent.setChatMemory(memory);
        agent.setConversationId("task-chat-1");

        assertEquals("second answer", agent.getLastAssistantMemoryText());
    }

    private static class PublishableResultAgent extends BaseAgent {

        private int stepCount;

        @Override
        public String step() {
            stepCount++;
            if (stepCount == 1) {
                return "tool progress";
            }
            setState(AgentState.FINISHED);
            return "final answer";
        }

        @Override
        protected boolean shouldPublishStepResult(String stepResult) {
            return !"tool progress".equals(stepResult);
        }
    }

    private static class MemoryAwareAgent extends BaseAgent {

        @Override
        public String step() {
            setState(AgentState.FINISHED);
            return "history size: " + getMessageList().size();
        }
    }

    private static class RecordingChatMemory implements ChatMemory {

        private final List<Message> history = new ArrayList<>();
        private String savedConversationId;
        private List<Message> savedMessages = new ArrayList<>();

        @Override
        public void add(String conversationId, Message message) {
            savedConversationId = conversationId;
            savedMessages = List.of(message);
        }

        @Override
        public void add(String conversationId, List<Message> messages) {
            savedConversationId = conversationId;
            savedMessages = new ArrayList<>(messages);
        }

        @Override
        public List<Message> get(String conversationId, int lastN) {
            return history.stream()
                    .skip(Math.max(0, history.size() - lastN))
                    .toList();
        }

        @Override
        public void clear(String conversationId) {
            history.clear();
        }
    }
}
