package com.agenthub.server.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ResearchReportAgentTest {

    @Test
    void configuresResearchReportPrompts() {
        ResearchReportAgent agent = new ResearchReportAgent(new ToolCallback[0], mock(ChatModel.class));

        assertEquals("ResearchReportAgent", agent.getName());
        assertTrue(agent.getSystemPrompt().contains("你是 AgentHub 的研究报告助手"));
        assertTrue(agent.getSystemPrompt().contains("默认使用用户输入的语言回复"));
        assertTrue(agent.getSystemPrompt().contains("用户使用中文时，全程使用中文"));
        assertTrue(agent.getSystemPrompt().contains("不要主动生成 PDF"));
        assertTrue(agent.getSystemPrompt().contains("仅支持 PDF"));
        assertTrue(agent.getSystemPrompt().contains("Word、Excel、PPT、Markdown、TXT、DOCX"));
        assertTrue(agent.getNextStepPrompt().contains("搜索相关网页资料"));
        assertTrue(agent.getNextStepPrompt().contains("不要在正文里添加 PDF 下载链接"));
        assertTrue(agent.getNextStepPrompt().contains("非 PDF 文档生成请求"));
        assertFalse(agent.getNextStepPrompt().contains("generate a PDF report"));
    }

    @Test
    void removesGeneratedPdfClaimWhenPdfToolDidNotSucceed() {
        ResearchReportAgent agent = new ResearchReportAgent(new ToolCallback[0], mock(ChatModel.class));

        String result = agent.normalizeFinalResponse("""
                # Research Report

                Findings go here.

                ---

                **Generated PDF Report**:
                [AI_Agent_Trends_2026_Report.pdf](./AI_Agent_Trends_2026_Report.pdf)

                The PDF has been generated and is ready for download.
                """);

        assertTrue(result.contains("Findings go here."));
        assertFalse(result.contains("Generated PDF Report"));
        assertFalse(result.contains("AI_Agent_Trends_2026_Report.pdf"));
        assertFalse(result.contains("PDF was not generated in this run."));
    }

    @Test
    void enablesPseudoToolFallbackForResearchTools() {
        ResearchReportAgent agent = new ResearchReportAgent(new ToolCallback[0], mock(ChatModel.class));

        assertTrue(agent.getPseudoToolCallFallbackToolNames().contains("searchWeb"));
        assertTrue(agent.getPseudoToolCallFallbackToolNames().contains("scrapeWebPage"));
        assertEquals("研究报告助手正在调用工具，请稍后重试。", agent.normalizeFinalResponse("""
                <search>
                {"name": "searchWeb", "arguments": {"query": "创业方向的机会和风险"}}
                </search>
                """));
    }

    @Test
    void keepsGeneratedPdfClaimWhenPdfToolSucceeded() {
        ResearchReportAgent agent = new ResearchReportAgent(new ToolCallback[0], mock(ChatModel.class));
        agent.recordToolResponses(new ToolResponseMessage(List.of(
                new ToolResponseMessage.ToolResponse(
                        "1",
                        "generatePDF",
                        "PDF generated successfully to: D:\\agent\\tmp\\pdf\\report.pdf"
                )
        )));

        String result = agent.normalizeFinalResponse("""
                # Research Report

                **Generated PDF Report**:
                [report.pdf](./report.pdf)
                """);

        assertTrue(result.contains("Generated PDF Report"));
        assertTrue(result.contains("report.pdf"));
    }
}


