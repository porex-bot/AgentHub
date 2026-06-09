package com.agenthub.server.tools;

import com.agenthub.server.constant.FileConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty(FileConstant.FILE_SAVE_DIR_PROPERTY);
    }

    @Test
    void generatePDF() {

        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        String filename = "test.pdf";
        String content = "This is a test PDF.";
        String result = pdfGenerationTool.generatePDF(filename, content);
        assertNotNull( result);
    }

    @Test
    void generatePdfFileUsesConfiguredSaveDirectory() {
        System.setProperty(FileConstant.FILE_SAVE_DIR_PROPERTY, tempDir.toString());
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

        PDFGenerationTool.PDFGenerationResult result = pdfGenerationTool.generatePdfFile("report.pdf", "报告正文");

        assertTrue(result.success());
        assertEquals("report.pdf", result.fileName());
        assertEquals(tempDir.resolve("pdf").resolve("report.pdf"), result.filePath());
        assertTrue(Files.exists(result.filePath()));
    }

    @Test
    void generatePdfFileSanitizesEmojiAndGeneratesPdf() {
        System.setProperty(FileConstant.FILE_SAVE_DIR_PROPERTY, tempDir.toString());
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

        assertDoesNotThrow(() -> {
            PDFGenerationTool.PDFGenerationResult result = pdfGenerationTool.generatePdfFile(
                    "emoji-report.pdf",
                    "### 🌟 通用三天项目开发计划\n- ✅ 目标\n- 📌 任务\n- 📤 交付物"
            );

            assertTrue(result.success());
            assertEquals("emoji-report.pdf", result.fileName());
            assertNull(result.errorMessage());
            assertTrue(Files.exists(result.filePath()));
        });
    }
}

