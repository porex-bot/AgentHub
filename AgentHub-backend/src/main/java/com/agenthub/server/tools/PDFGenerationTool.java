package com.agenthub.server.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.agenthub.server.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class PDFGenerationTool {

    private static final Map<Integer, String> PDF_TEXT_REPLACEMENTS = Map.ofEntries(
            Map.entry(0x2705, "[完成]"),
            Map.entry(0x1F31F, "*"),
            Map.entry(0x1F4CC, "[任务]"),
            Map.entry(0x1F4E4, "[交付]"),
            Map.entry(0x1F447, "[下一步]"),
            Map.entry(0x1F4C5, "[日期]"),
            Map.entry(0x1F552, "[时间]"),
            Map.entry(0x1F527, "[工具]")
    );

    public record PDFGenerationResult(boolean success, String fileName, Path filePath, String errorMessage) {
    }

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        PDFGenerationResult result = generatePdfFile(fileName, content);
        if (result.success()) {
            return "PDF generated successfully to: " + result.filePath();
        }
        return "Error generating PDF: " + result.errorMessage();
    }

    public PDFGenerationResult generatePdfFile(String fileName, String content) {
        String safeFileName = sanitizeFileName(fileName);
        Path fileDir = Path.of(FileConstant.fileSaveDir(), "pdf");
        Path filePath = fileDir.resolve(safeFileName);
        try {
            FileUtil.mkdir(fileDir.toString());
            try (PdfWriter writer = new PdfWriter(filePath.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                Paragraph paragraph = new Paragraph(toPdfCompatibleText(content));
                document.add(paragraph);
            }
            return new PDFGenerationResult(true, safeFileName, filePath, null);
        } catch (Exception e) {
            FileUtil.del(filePath.toString());
            return new PDFGenerationResult(false, safeFileName, filePath, e.getMessage());
        }
    }

    public static String sanitizeFileName(String fileName) {
        String value = fileName == null ? "" : fileName.trim();
        value = value.replace('\\', '/');
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(slashIndex + 1);
        }
        value = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (value.isBlank()) {
            value = "report.pdf";
        }
        if (!value.toLowerCase().endsWith(".pdf")) {
            value = value + ".pdf";
        }
        return value;
    }

    private static String toPdfCompatibleText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(content.length());
        for (int offset = 0; offset < content.length(); ) {
            int codePoint = content.codePointAt(offset);
            String replacement = PDF_TEXT_REPLACEMENTS.get(codePoint);
            if (replacement != null) {
                sanitized.append(replacement);
            } else if (isPdfCompatibleCodePoint(codePoint)) {
                sanitized.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return sanitized.toString();
    }

    private static boolean isPdfCompatibleCodePoint(int codePoint) {
        if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
            return true;
        }
        if (codePoint > Character.MAX_VALUE) {
            return false;
        }

        int type = Character.getType(codePoint);
        return type != Character.OTHER_SYMBOL
                && type != Character.SURROGATE
                && type != Character.PRIVATE_USE
                && type != Character.UNASSIGNED;
    }
}


