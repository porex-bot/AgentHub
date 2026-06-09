package com.agenthub.server.agent;

import com.agenthub.server.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Set;

/**
 * AgentHub 通用任务智能体，支持自主规划和工具调用。
 */
public class TaskExecutionAgent extends ToolCallAgent {
    public TaskExecutionAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(filterInternalTools(allTools));
        this.setName("TaskExecutionAgent");
        String systemPrompt = """
                你是 AgentHub 的通用任务智能体。
                你的职责是帮助用户把目标拆成清晰的执行计划，在确实有用时调用工具，并返回简洁的最终结果。
                务实推进：必要时澄清假设，避免不必要的工具调用，并在关键进展处告知用户。
                除非已有来自推理或工具结果的充分依据，否则不要声称任务已经完成。
                如果请求存在安全风险、破坏性操作，或需要你没有的凭证和私有访问权限，请说明限制并给出更安全的下一步。
                文档生成能力仅支持 PDF。用户要求生成 Word、Excel、PPT、Markdown、TXT、DOCX 或其他非 PDF 文档时，明确告知当前仅支持 PDF，不要尝试生成或声称已生成其他格式。
                """;
        this.setSystemPrompt(systemPrompt);
        String nextStepPrompt = """
                按以下顺序决定下一步：
                1. 理解用户目标和当前状态。
                2. 如果可以直接回答，就直接回答。
                3. 如果需要外部信息、文件操作、命令执行或生成结果，选择最小且有用的工具调用。
                4. 每次获得工具结果后，概括发生了什么，并判断是否仍需继续。
                5. 当证据足够时，停止调用工具并给出最终回答。
                遇到非 PDF 文档生成请求时，直接说明当前仅支持 PDF，可提供正文或 PDF 导出。
                收集信息后不要调用 `terminate` 工具；请通过正常助手回复结束。
                """;
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    static ToolCallback[] filterInternalTools(ToolCallback[] allTools) {
        if (allTools == null) {
            return new ToolCallback[0];
        }
        return Arrays.stream(allTools)
                .filter(tool -> !"doTerminate".equals(tool.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    @Override
    protected Set<String> getPromisedToolCallFallbackToolNames() {
        return Set.of("searchWeb");
    }
}


