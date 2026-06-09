package com.agenthub.server.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.agenthub.server.agent.model.AgentState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern PSEUDO_TOOL_TAG_PATTERN = Pattern.compile(
            "(?is)^\\s*<([a-z][\\w-]*)>\\s*(\\{.*})\\s*</\\1>\\s*$"
    );

    private static final Pattern PROMISED_SEARCH_VERB_BEFORE_QUERY_PATTERN = Pattern.compile(
            "(?is).*(?:开始|将|准备|尝试|继续).{0,20}(?:搜索|检索|查找).{0,20}[\"“]([^\"”]+)[\"”].*"
    );

    private static final Pattern PROMISED_SEARCH_QUERY_BEFORE_VERB_PATTERN = Pattern.compile(
            "(?is).*(?:关键词|关键字|query).{0,10}[\"“]([^\"”]+)[\"”].{0,30}(?:搜索|检索|查找).*"
    );

    private static final int MAX_PSEUDO_TOOL_FALLBACKS = 3;

    private final ToolCallback[] availableTools;

    private ChatResponse toolCallChatResponse;

    private String finalResponse;

    private boolean currentStepPublishable;

    private int pseudoToolFallbackCount;

    private final ToolCallingManager toolCallingManager;

    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    @Override
    public boolean think() {
        this.currentStepPublishable = false;
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            String result = assistantMessage.getText();
            log.info("{} thinking result: {}", getName(), result);
            log.info("{} selected {} tool calls", getName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("tool: %s, args: %s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            if (toolCallList.isEmpty()) {
                if (handlePseudoToolCallIfPresent(result) || handlePromisedToolCallIfPresent(result)) {
                    return true;
                }
                String normalizedResult = normalizeFinalResponse(result);
                this.finalResponse = normalizedResult;
                this.currentStepPublishable = true;
                getMessageList().add(new AssistantMessage(normalizedResult));
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("{} failed while thinking: {}", getName(), e.getMessage(), e);
            getMessageList().add(new AssistantMessage("智能体执行失败：" + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有需要执行的工具调用。";
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        recordToolResponses(toolResponseMessage);

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled && StrUtil.isNotBlank(finalResponse)) {
            setState(AgentState.FINISHED);
            currentStepPublishable = true;
        }

        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "tool " + response.name() + " returned: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return formatToolResultsForDisplay(toolResponseMessage);
    }

    protected String formatToolResultsForDisplay(ToolResponseMessage toolResponseMessage) {
        String toolNames = toolResponseMessage.getResponses().stream()
                .map(ToolResponseMessage.ToolResponse::name)
                .distinct()
                .collect(Collectors.joining(", "));
        if (StrUtil.isBlank(toolNames)) {
            return "工具调用完成，但没有返回可展示的结果。";
        }
        return "已调用工具：" + toolNames + "，正在整理结果。";
    }

    protected void recordToolResponses(ToolResponseMessage toolResponseMessage) {
    }

    protected Set<String> getPseudoToolCallFallbackToolNames() {
        return Set.of();
    }

    protected Set<String> getPromisedToolCallFallbackToolNames() {
        return Set.of();
    }

    protected boolean handlePseudoToolCallIfPresent(String response) {
        Optional<PseudoToolCall> pseudoToolCall = parsePseudoToolCall(response);
        if (pseudoToolCall.isEmpty()) {
            return false;
        }
        return executeFallbackToolCall(pseudoToolCall.get(), getPseudoToolCallFallbackToolNames());
    }

    protected boolean handlePromisedToolCallIfPresent(String response) {
        Optional<PseudoToolCall> promisedToolCall = parsePromisedSearchCall(response);
        if (promisedToolCall.isEmpty()) {
            return false;
        }
        return executeFallbackToolCall(promisedToolCall.get(), getPromisedToolCallFallbackToolNames());
    }

    private boolean executeFallbackToolCall(PseudoToolCall toolCall, Set<String> allowedToolNames) {
        if (pseudoToolFallbackCount >= MAX_PSEUDO_TOOL_FALLBACKS) {
            log.warn("{} ignored pseudo tool call after reaching fallback limit", getName());
            return false;
        }

        if (!allowedToolNames.contains(toolCall.name())) {
            log.warn("{} ignored unsupported pseudo tool call: {}", getName(), toolCall.name());
            return false;
        }

        Optional<ToolCallback> toolCallback = findToolCallback(toolCall.name());
        if (toolCallback.isEmpty()) {
            log.warn("{} could not find pseudo tool callback: {}", getName(), toolCall.name());
            return false;
        }

        pseudoToolFallbackCount++;
        log.info("{} executing fallback tool call: {}, args: {}", getName(), toolCall.name(), toolCall.argumentsJson());
        String toolResult;
        try {
            toolResult = toolCallback.get().call(toolCall.argumentsJson());
        } catch (Exception e) {
            log.warn("{} failed to execute pseudo tool call {}: {}", getName(), toolCall.name(), e.getMessage(), e);
            toolResult = "工具 " + toolCall.name() + " 调用失败：" + e.getMessage();
        }
        log.info("fallback tool {} returned: {}", toolCall.name(), toolResult);

        getMessageList().add(new UserMessage("""
                系统已代为执行模型输出的伪工具调用，请继续完成用户请求，不要再输出伪工具标签。
                工具名称：%s
                工具参数：%s
                工具 %s 返回：
                %s
                """.formatted(toolCall.name(), toolCall.argumentsJson(), toolCall.name(), toolResult)));
        this.currentStepPublishable = false;
        this.finalResponse = null;
        return true;
    }

    protected String normalizeFinalResponse(String response) {
        if (parsePseudoToolCall(response).isPresent()) {
            return getPseudoToolCallFallbackMessage();
        }
        return response;
    }

    protected String getPseudoToolCallFallbackMessage() {
        return "正在调用工具，请稍后重试。";
    }

    private Optional<ToolCallback> findToolCallback(String name) {
        return Arrays.stream(availableTools)
                .filter(tool -> tool.getToolDefinition() != null)
                .filter(tool -> name.equals(tool.getToolDefinition().name()))
                .findFirst();
    }

    private Optional<PseudoToolCall> parsePseudoToolCall(String response) {
        if (StrUtil.isBlank(response)) {
            return Optional.empty();
        }
        Matcher matcher = PSEUDO_TOOL_TAG_PATTERN.matcher(response);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String tagName = matcher.group(1);
        String payload = matcher.group(2).trim();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            String toolName = root.hasNonNull("name") ? root.get("name").asText() : pseudoTagToToolName(tagName);
            JsonNode arguments = root.get("arguments");
            if (arguments == null || arguments.isNull()) {
                arguments = root.has("query") || root.has("url") ? root : OBJECT_MAPPER.createObjectNode();
            }
            return Optional.of(new PseudoToolCall(toolName, OBJECT_MAPPER.writeValueAsString(arguments)));
        } catch (Exception e) {
            log.warn("{} failed to parse pseudo tool call: {}", getName(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<PseudoToolCall> parsePromisedSearchCall(String response) {
        if (StrUtil.isBlank(response)) {
            return Optional.empty();
        }
        Matcher matcher = PROMISED_SEARCH_VERB_BEFORE_QUERY_PATTERN.matcher(response);
        if (!matcher.matches()) {
            matcher = PROMISED_SEARCH_QUERY_BEFORE_VERB_PATTERN.matcher(response);
        }
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String query = matcher.group(1);
        if (StrUtil.isBlank(query)) {
            return Optional.empty();
        }
        try {
            JsonNode arguments = OBJECT_MAPPER.createObjectNode().put("query", query.trim());
            return Optional.of(new PseudoToolCall("searchWeb", OBJECT_MAPPER.writeValueAsString(arguments)));
        } catch (Exception e) {
            log.warn("{} failed to build promised search call: {}", getName(), e.getMessage());
            return Optional.empty();
        }
    }

    private String pseudoTagToToolName(String tagName) {
        if ("search".equalsIgnoreCase(tagName)) {
            return "searchWeb";
        }
        if ("scrape".equalsIgnoreCase(tagName)) {
            return "scrapeWebPage";
        }
        return tagName;
    }

    private record PseudoToolCall(String name, String argumentsJson) {
    }

    @Override
    protected String getNoActionResult() {
        if (StrUtil.isNotBlank(finalResponse)) {
            return finalResponse;
        }
        return super.getNoActionResult();
    }

    @Override
    protected boolean shouldPublishStepResult(String stepResult) {
        return currentStepPublishable && StrUtil.isNotBlank(stepResult);
    }
}
