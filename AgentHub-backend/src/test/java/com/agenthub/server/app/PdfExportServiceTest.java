package com.agenthub.server.app;

import com.agenthub.server.tools.PDFGenerationTool;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExportServiceTest {

    @Test
    void detectsShortPdfExportRequests() {
        assertTrue(PdfExportService.isPdfExportRequest("pdf"));
        assertTrue(PdfExportService.isPdfExportRequest("生成 PDF"));
        assertTrue(PdfExportService.isPdfExportRequest("帮我导出pdf"));
        assertTrue(PdfExportService.isPdfExportRequest("User task: pdf"));
        assertTrue(PdfExportService.isPdfExportRequest("请生成一份 PDF"));
        assertTrue(PdfExportService.isPdfExportRequest("输出为 PDF"));
        assertFalse(PdfExportService.isPdfExportRequest("什么是 PDF 文件"));
        assertFalse(PdfExportService.isPdfExportRequest("帮我规划一个三天项目开发计划"));
    }

    @Test
    void detectsUnsupportedDocumentExportRequests() {
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("生成md文档"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("我要 Markdown 文档"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("我要md文芳"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("md文档"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("导出 Word"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("生成 docx"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("输出 PPT"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("保存 txt"));
        assertTrue(PdfExportService.isUnsupportedDocumentExportRequest("User task: 生成md文档"));
        assertFalse(PdfExportService.isUnsupportedDocumentExportRequest("生成 PDF"));
        assertFalse(PdfExportService.isUnsupportedDocumentExportRequest("什么是 Markdown"));
        assertFalse(PdfExportService.isUnsupportedDocumentExportRequest("分析md文档"));
        assertFalse(PdfExportService.isUnsupportedDocumentExportRequest("用 Markdown 格式列出计划"));
        assertFalse(PdfExportService.isUnsupportedDocumentExportRequest("继续优化计划"));
    }

    @Test
    void exportIfRequestedReturnsDownloadUrlAndOriginalContentWhenPdfSucceeds() {
        RecordingPDFGenerationTool pdfTool = RecordingPDFGenerationTool.success("report.pdf");
        PdfExportService service = new PdfExportService(pdfTool);

        Optional<PdfExportService.ExportResult> result = service.exportIfRequested(
                "生成PDF",
                "chat-1",
                "通用三天项目开发计划"
        );

        assertTrue(result.isPresent());
        assertTrue(result.get().success());
        assertEquals("通用三天项目开发计划", result.get().originalContent());
        assertEquals("PDF 已生成，可点击链接下载。", result.get().statusMessage());
        assertNotNull(result.get().downloadUrl());
        assertTrue(result.get().downloadUrl().startsWith("/api/ai/files/pdf/"));
        assertEquals("通用三天项目开发计划", pdfTool.capturedContent);
        assertTrue(pdfTool.capturedFileName.endsWith(".pdf"));
    }

    @Test
    void exportIfRequestedKeepsOriginalContentWhenPdfFails() {
        RecordingPDFGenerationTool pdfTool = RecordingPDFGenerationTool.failure("字体加载失败");
        PdfExportService service = new PdfExportService(pdfTool);

        Optional<PdfExportService.ExportResult> result = service.exportIfRequested(
                "pdf",
                "chat-1",
                "研究报告正文"
        );

        assertTrue(result.isPresent());
        assertFalse(result.get().success());
        assertEquals("研究报告正文", result.get().originalContent());
        assertEquals("PDF 生成失败：字体加载失败", result.get().statusMessage());
        assertNull(result.get().downloadUrl());
    }

    @Test
    void exportIfRequestedReturnsFailureWhenNoPreviousAssistantContentExists() {
        PdfExportService service = new PdfExportService(RecordingPDFGenerationTool.success("report.pdf"));

        Optional<PdfExportService.ExportResult> result = service.exportIfRequested("pdf", "chat-1", "");

        assertTrue(result.isPresent());
        assertFalse(result.get().success());
        assertEquals("", result.get().originalContent());
        assertEquals("PDF 生成失败：没有找到上一轮可导出的报告或计划内容。", result.get().statusMessage());
        assertNull(result.get().downloadUrl());
    }

    @Test
    void exportIfRequestedIgnoresNonPdfRequests() {
        PdfExportService service = new PdfExportService(RecordingPDFGenerationTool.success("report.pdf"));

        Optional<PdfExportService.ExportResult> result = service.exportIfRequested("继续优化计划", "chat-1", "计划正文");

        assertTrue(result.isEmpty());
    }

    private static class RecordingPDFGenerationTool extends PDFGenerationTool {
        private final PDFGenerationTool.PDFGenerationResult result;
        private String capturedFileName;
        private String capturedContent;

        private RecordingPDFGenerationTool(PDFGenerationTool.PDFGenerationResult result) {
            this.result = result;
        }

        private static RecordingPDFGenerationTool success(String fileName) {
            return new RecordingPDFGenerationTool(
                    new PDFGenerationTool.PDFGenerationResult(true, fileName, Path.of("pdf", fileName), null)
            );
        }

        private static RecordingPDFGenerationTool failure(String errorMessage) {
            return new RecordingPDFGenerationTool(
                    new PDFGenerationTool.PDFGenerationResult(false, "report.pdf", null, errorMessage)
            );
        }

        @Override
        public PDFGenerationTool.PDFGenerationResult generatePdfFile(String fileName, String content) {
            this.capturedFileName = fileName;
            this.capturedContent = content;
            return result;
        }
    }
}
