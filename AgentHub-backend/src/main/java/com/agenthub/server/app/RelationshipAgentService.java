package com.agenthub.server.app;

import com.agenthub.server.advisor.MyLoggerAdvisor;
import com.agenthub.server.chatMemory.ChatMemoryPaths;
import com.agenthub.server.chatMemory.FileBasedChatMemory;
import com.agenthub.server.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class RelationshipAgentService {
    @Resource
    private VectorStore relationshipVectorStore;

    @Resource
    private Advisor relationshipRagCloudAdvisor;
    @Resource
    private QueryRewriter queryRewriter;
    @Resource
    private VectorStore pgVectorVectorStore;
    @Resource
    private ToolCallback[] allTools;
    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是 AgentHub 的关系咨询助手，专注恋爱、亲密关系、沟通冲突和关系决策。
            你的回答应当温和、具体、可操作，优先帮助用户澄清问题、识别情绪和需求，再给出分步建议。
            不要制造焦虑，不要进行道德审判，不要承诺能操控他人情感。
            如果信息不足，先给出需要追问的关键点，再提供通用建议。
            当问题涉及暴力、自我伤害、严重控制或跟踪时，优先建议用户保护人身安全并寻求现实支持。
            """;

    record RelationshipReport(String title, List<String> suggestions) {
    }

    public RelationshipAgentService(ChatModel dashscopeChatModel) {
        String fileDir = ChatMemoryPaths.directory("relationship").toString();
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String chat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("message: {}", content);
        return content;
    }

    public Flux<String> streamChat(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    public RelationshipReport generateRelationshipReport(String message, String chatId) {
        RelationshipReport relationshipReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + """
                        请将用户的描述转化为一份简短的关系建议报告。
                        只返回 JSON，格式为：{"title":"标题","suggestions":["建议1","建议2","建议3"]}
                        title 应概括用户的核心关系议题，suggestions 应为可执行的短建议。
                        """)
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(RelationshipReport.class);

        log.info("relationshipReport: {}", relationshipReport);
        return relationshipReport;
    }

    public String chatWithKnowledgeBase(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse response = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(relationshipVectorStore))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("message: {}", content);
        return content;
    }

    public String chatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("message: {}", content);
        return content;
    }

    public String chatWithMcpTools(String message, String chatId) {
        if (toolCallbackProvider == null) {
            return "MCP 工具未启用，请检查 MCP 客户端配置。";
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
