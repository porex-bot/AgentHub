package com.agenthub.server.agent;

import cn.hutool.core.util.StrUtil;
import com.agenthub.server.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {
    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private int maxSteps = 10;
    private int currentStep = 0;
    private ChatClient chatClient;
    private List<Message> messageList = new ArrayList<>();
    private ChatMemory chatMemory;
    private String conversationId;
    private int memoryRetrieveSize = 10;

    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        state = AgentState.RUNNING;
        initializeMessageList(userPrompt);
        List<String> results = new ArrayList<>();
        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                log.info("Executing step {}/{}", currentStep, maxSteps);
                String stepResult = step();
                if (shouldPublishStepResult(stepResult)) {
                    results.add(stepResult);
                }
            }
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("执行结束：达到最大步骤数（" + maxSteps + "）");
            }
            saveConversationMemory(userPrompt, results);
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误：" + e.getMessage();
        } finally {
            this.clearup();
        }
    }

    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从当前状态运行智能体：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：用户输入为空，无法运行智能体。");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }

            state = AgentState.RUNNING;
            initializeMessageList(userPrompt);
            List<String> results = new ArrayList<>();
            try {
                while (currentStep < maxSteps && state != AgentState.FINISHED) {
                    currentStep++;
                    log.info("Executing step {}/{}", currentStep, maxSteps);
                    String stepResult = step();
                    if (shouldPublishStepResult(stepResult)) {
                        results.add(stepResult);
                        sseEmitter.send(stepResult);
                    }
                }
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    String message = "执行结束：达到最大步骤数（" + maxSteps + "）";
                    results.add(message);
                    sseEmitter.send(message);
                }
                saveConversationMemory(userPrompt, results);
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                this.clearup();
            }
        });

        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.clearup();
            log.warn("SSE connection timeout");
        });
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.clearup();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    public abstract String step();

    protected boolean shouldPublishStepResult(String stepResult) {
        return StrUtil.isNotBlank(stepResult);
    }

    public String getLastAssistantMemoryText() {
        if (chatMemory == null || StrUtil.isBlank(conversationId)) {
            return "";
        }
        List<Message> history = chatMemory.get(conversationId, memoryRetrieveSize);
        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (message instanceof AssistantMessage assistantMessage && StrUtil.isNotBlank(assistantMessage.getText())) {
                return assistantMessage.getText();
            }
        }
        return "";
    }

    private void initializeMessageList(String userPrompt) {
        List<Message> history = loadConversationMemory();
        messageList = new ArrayList<>(history);
        messageList.add(new UserMessage(userPrompt));
    }

    private List<Message> loadConversationMemory() {
        if (chatMemory == null || StrUtil.isBlank(conversationId)) {
            return List.of();
        }
        return chatMemory.get(conversationId, memoryRetrieveSize);
    }

    private void saveConversationMemory(String userPrompt, List<String> results) {
        if (chatMemory == null || StrUtil.isBlank(conversationId)) {
            return;
        }
        String finalAnswer = String.join("\n", results).trim();
        if (StrUtil.isBlank(finalAnswer)) {
            return;
        }
        chatMemory.add(conversationId, List.of(
                new UserMessage(userPrompt),
                new AssistantMessage(finalAnswer)
        ));
    }

    protected void clearup() {
    }
}
