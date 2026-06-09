package com.agenthub.server.agent;

import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.Set;
import java.util.regex.Pattern;

public class ResearchReportAgent extends TaskExecutionAgent {

    private static final Pattern GENERATED_PDF_REPORT_SECTION = Pattern.compile(
            "(?is)\\R?---\\s*\\R+\\*\\*Generated PDF Report\\*\\*:\\s*.*\\z"
    );
    private static final Pattern GENERATED_PDF_REPORT_HEADING = Pattern.compile(
            "(?is)\\R{0,2}\\*\\*Generated PDF Report\\*\\*:\\s*.*\\z"
    );

    private boolean pdfGenerated;

    public ResearchReportAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools, dashscopeChatModel);
        this.setName("ResearchReportAgent");
        this.setSystemPrompt("""
                你是 AgentHub 的研究报告助手。
                默认使用用户输入的语言回复；用户使用中文时，全程使用中文，包括报告标题、工具状态、限制说明和结论。
                你的职责是围绕用户主题进行网页搜索、必要网页抓取、证据整理和结构化报告输出。
                优先使用可靠、近期、直接相关的网页资料。区分事实、判断和建议，不要编造引用、来源名称、日期或工具结果。
                如果搜索没有返回有效网页结果，要明确说明检索结果为空；没有网页证据时，不要列出具体机构报告或来源名称。
                不要主动生成 PDF，也不要在正文中声明 PDF 已生成、添加本地 PDF 链接或下载就绪声明。PDF 导出由系统在用户明确要求时处理；如果没有成功的 PDF 附件事件，只输出原文。
                文档生成能力仅支持 PDF。用户要求生成 Word、Excel、PPT、Markdown、TXT、DOCX 或其他非 PDF 文档时，明确告知当前仅支持 PDF，不要尝试生成或声称已生成其他格式。
                报告结构保持清晰：研究目标、方法、关键发现、证据摘要、风险或限制、建议、下一步。
                """);
        this.setNextStepPrompt("""
                按以下研究流程推进：
                1. 明确研究主题、范围和必要假设。
                2. 搜索相关网页资料，优先获取新近外部信息。
                3. 只有需要来源细节时才抓取网页。
                4. 对资料进行比较、提炼和综合，不要堆砌原始搜索结果。
                5. 输出结构化研究报告正文。
                6. 不要主动调用 PDF 生成工具，不要在正文里添加 PDF 下载链接；用户明确请求 PDF 时，由系统导出上一轮正文并发送附件。
                遇到非 PDF 文档生成请求时，直接说明当前仅支持 PDF，可提供正文或 PDF 导出。
                使用工具后，直接给用户最终报告正文，不要进行不必要的额外工具调用。
                收集信息后不要调用 `terminate` 工具；请通过正常助手回复结束。
                   """);
    }

    @Override
    protected void recordToolResponses(ToolResponseMessage toolResponseMessage) {
        boolean generated = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "generatePDF".equals(response.name())
                        && response.responseData() != null
                        && response.responseData().startsWith("PDF generated successfully"));
        if (generated) {
            pdfGenerated = true;
        }
    }

    @Override
    protected Set<String> getPseudoToolCallFallbackToolNames() {
        return Set.of("searchWeb", "scrapeWebPage");
    }

    @Override
    protected String getPseudoToolCallFallbackMessage() {
        return "研究报告助手正在调用工具，请稍后重试。";
    }

    @Override
    protected String normalizeFinalResponse(String response) {
        String normalized = super.normalizeFinalResponse(response);
        if (normalized == null || !normalized.equals(response)) {
            return normalized;
        }
        if (pdfGenerated || response == null || response.isBlank()) {
            return response;
        }

        String sanitized = GENERATED_PDF_REPORT_SECTION.matcher(response).replaceFirst("");
        sanitized = GENERATED_PDF_REPORT_HEADING.matcher(sanitized).replaceFirst("");
        if (sanitized.equals(response)) {
            return response;
        }

        return sanitized.stripTrailing();
    }
}
