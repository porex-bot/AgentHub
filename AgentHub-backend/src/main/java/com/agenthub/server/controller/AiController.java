package com.agenthub.server.controller;

import com.agenthub.server.agent.ResearchReportAgent;
import com.agenthub.server.agent.TaskExecutionAgent;
import com.agenthub.server.app.PdfExportService;
import com.agenthub.server.app.RelationshipAgentService;
import com.agenthub.server.app.SseEventFormatter;
import com.agenthub.server.chatMemory.ChatMemoryPaths;
import com.agenthub.server.chatMemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    private RelationshipAgentService relationshipAgentService;
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ChatModel dashscopeChatModel;
    @Resource
    private PdfExportService pdfExportService;

    private final ChatMemory taskAgentChatMemory = new FileBasedChatMemory(
            ChatMemoryPaths.directory("task").toString()
    );
    private final ChatMemory reportChatMemory = new FileBasedChatMemory(
            ChatMemoryPaths.directory("report").toString()
    );

    @RequestMapping("/agent/chat/sync")
    public String chatWithRelationshipAgent(String message, String chatId) {
        return relationshipAgentService.chat(message, chatId);
    }

    @RequestMapping(value = "/agent/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCompatibilityRelationshipChat(String message, String chatId) {
        return streamRelationshipChat(message, chatId);
    }

    @RequestMapping(value = "/agents/relationship/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamRelationshipChat(String message, String chatId) {
        return relationshipAgentService.streamChat(message, chatId);
    }

    @GetMapping(value = "/task/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompatibilityTaskAgentChat(String message, String chatId) {
        return streamTaskAgentChat(message, chatId);
    }

    @GetMapping(value = "/agents/task/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTaskAgentChat(String message, String chatId) {
        if (PdfExportService.isUnsupportedDocumentExportRequest(message)) {
            return streamContent(PdfExportService.UNSUPPORTED_DOCUMENT_EXPORT_MESSAGE);
        }
        TaskExecutionAgent taskAgent = createTaskExecutionAgent(chatId);
        Optional<PdfExportService.ExportResult> pdfExport = pdfExportService.exportIfRequested(
                message,
                chatId,
                taskAgent.getLastAssistantMemoryText()
        );
        if (pdfExport.isPresent()) {
            return streamPdfExport(pdfExport.get());
        }
        return taskAgent.runStream(message);
    }

    @GetMapping(value = "/report/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCompatibilityResearchReportChat(String message, String chatId) {
        return streamResearchReportChat(message, chatId);
    }

    @GetMapping(value = "/agents/research/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearchReportChat(String message, String chatId) {
        if (PdfExportService.isUnsupportedDocumentExportRequest(message)) {
            return streamContent(PdfExportService.UNSUPPORTED_DOCUMENT_EXPORT_MESSAGE);
        }
        ResearchReportAgent reportAgent = createResearchReportAgent(chatId);
        Optional<PdfExportService.ExportResult> pdfExport = pdfExportService.exportIfRequested(
                message,
                chatId,
                reportAgent.getLastAssistantMemoryText()
        );
        if (pdfExport.isPresent()) {
            return streamPdfExport(pdfExport.get());
        }
        return reportAgent.runStream(message);
    }

    @GetMapping("/files/pdf/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadPdf(@PathVariable String fileName) {
        if (!pdfExportService.isSafePdfFileName(fileName)) {
            return ResponseEntity.badRequest().build();
        }

        Path pdfPath = pdfExportService.resolvePdfPath(fileName);
        if (!Files.exists(pdfPath) || !Files.isRegularFile(pdfPath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            org.springframework.core.io.Resource resource =
                    new org.springframework.core.io.UrlResource(pdfPath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private TaskExecutionAgent createTaskExecutionAgent(String chatId) {
        TaskExecutionAgent taskAgent = new TaskExecutionAgent(allTools, dashscopeChatModel);
        taskAgent.setChatMemory(taskAgentChatMemory);
        taskAgent.setConversationId(chatId);
        return taskAgent;
    }

    private ResearchReportAgent createResearchReportAgent(String chatId) {
        ResearchReportAgent reportAgent = new ResearchReportAgent(allTools, dashscopeChatModel);
        reportAgent.setChatMemory(reportChatMemory);
        reportAgent.setConversationId(chatId);
        return reportAgent;
    }

    private SseEmitter streamPdfExport(PdfExportService.ExportResult result) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (result.originalContent() != null && !result.originalContent().isBlank()) {
                    sseEmitter.send(SseEmitter.event().data(SseEventFormatter.content(result.originalContent())));
                }
                sseEmitter.send(SseEmitter.event().data(SseEventFormatter.content("\n\n" + result.statusMessage())));
                if (result.success()) {
                    sseEmitter.send(SseEmitter.event().data(SseEventFormatter.attachment("下载 PDF", result.downloadUrl(), result.fileName())));
                }
                sseEmitter.complete();
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
            }
        });
        return sseEmitter;
    }

    private SseEmitter streamContent(String content) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            try {
                sseEmitter.send(SseEmitter.event().data(SseEventFormatter.content(content)));
                sseEmitter.complete();
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
            }
        });
        return sseEmitter;
    }
}


