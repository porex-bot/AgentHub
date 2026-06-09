package com.agenthub.server.app;

import cn.hutool.core.util.StrUtil;
import com.agenthub.server.constant.FileConstant;
import com.agenthub.server.tools.PDFGenerationTool;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class PdfExportService {

    public static final String UNSUPPORTED_DOCUMENT_EXPORT_MESSAGE = "当前仅支持 PDF 文档生成";

    private static final List<String> PDF_EXPORT_MARKERS = List.of(
            "生成pdf",
            "导出pdf",
            "下载pdf",
            "转pdf",
            "转成pdf",
            "输出pdf",
            "输出为pdf",
            "保存pdf",
            "pdf版"
    );
    private static final Pattern VERB_THEN_PDF_PATTERN = Pattern.compile("(生成|导出|下载|输出|保存|转成|转换|打印|制作).{0,8}pdf");
    private static final List<String> STRONG_DOCUMENT_EXPORT_VERBS = List.of(
            "生成",
            "导出",
            "下载",
            "输出",
            "保存",
            "转成",
            "转为",
            "转换",
            "打印",
            "制作",
            "创建"
    );
    private static final List<String> DESIRE_DOCUMENT_EXPORT_MARKERS = List.of(
            "我要",
            "想要",
            "给我",
            "帮我",
            "来一份",
            "弄一份"
    );
    private static final List<String> DOCUMENT_NOUN_MARKERS = List.of(
            "文档",
            "文件",
            "表格",
            "演示文稿",
            "版"
    );
    private static final List<String> NON_EXPORT_INTENT_MARKERS = List.of(
            "什么是",
            "是什么",
            "怎么",
            "如何",
            "学习",
            "了解",
            "解释",
            "介绍",
            "教程",
            "用法"
    );
    private static final Pattern UNSUPPORTED_DOCUMENT_FORMAT_PATTERN = Pattern.compile(
            "(?i)(markdown|word|excel|pptx|ppt|docx|xlsx|xls|txt|(?<![a-z0-9])md(?![a-z0-9])|(?<![a-z0-9])doc(?![a-z0-9]))"
    );
    private static final Pattern BARE_UNSUPPORTED_DOCUMENT_FORMAT_PATTERN = Pattern.compile(
            "(?i)^(markdown|word|excel|pptx|ppt|docx|xlsx|xls|txt|md|doc)(文档|文件|表格|演示文稿|版)?$"
    );
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PDFGenerationTool pdfGenerationTool;

    public PdfExportService() {
        this(new PDFGenerationTool());
    }

    PdfExportService(PDFGenerationTool pdfGenerationTool) {
        this.pdfGenerationTool = pdfGenerationTool;
    }

    public record ExportResult(
            boolean success,
            String originalContent,
            String statusMessage,
            String downloadUrl,
            String fileName
    ) {
    }

    public static boolean isPdfExportRequest(String prompt) {
        String compact = compact(stripFrontendPromptPrefix(prompt));
        if (compact.equals("pdf")) {
            return true;
        }
        return PDF_EXPORT_MARKERS.stream().anyMatch(compact::contains)
                || VERB_THEN_PDF_PATTERN.matcher(compact).find();
    }

    public static boolean isUnsupportedDocumentExportRequest(String prompt) {
        String compact = compact(stripFrontendPromptPrefix(prompt));
        if (StrUtil.isBlank(compact) || !UNSUPPORTED_DOCUMENT_FORMAT_PATTERN.matcher(compact).find()) {
            return false;
        }
        if (NON_EXPORT_INTENT_MARKERS.stream().anyMatch(compact::contains)) {
            return false;
        }

        boolean hasStrongVerb = STRONG_DOCUMENT_EXPORT_VERBS.stream().anyMatch(compact::contains);
        boolean hasDesireMarker = DESIRE_DOCUMENT_EXPORT_MARKERS.stream().anyMatch(compact::contains);
        boolean hasDocumentNoun = DOCUMENT_NOUN_MARKERS.stream().anyMatch(compact::contains);
        boolean bareFormatDocumentRequest = BARE_UNSUPPORTED_DOCUMENT_FORMAT_PATTERN.matcher(compact).matches();

        return hasStrongVerb || (hasDesireMarker && (hasDocumentNoun || compact.length() <= 24)) || bareFormatDocumentRequest;
    }

    public Optional<ExportResult> exportIfRequested(String prompt, String chatId, String previousAssistantContent) {
        if (!isPdfExportRequest(prompt)) {
            return Optional.empty();
        }

        String originalContent = previousAssistantContent == null ? "" : previousAssistantContent.trim();
        if (StrUtil.isBlank(originalContent)) {
            return Optional.of(new ExportResult(
                    false,
                    "",
                    "PDF 生成失败：没有找到上一轮可导出的报告或计划内容。",
                    null,
                    null
            ));
        }

        String fileName = buildFileName(chatId);
        PDFGenerationTool.PDFGenerationResult result = pdfGenerationTool.generatePdfFile(fileName, originalContent);
        if (!result.success()) {
            return Optional.of(new ExportResult(
                    false,
                    originalContent,
                    "PDF 生成失败：" + result.errorMessage(),
                    null,
                    result.fileName()
            ));
        }

        return Optional.of(new ExportResult(
                true,
                originalContent,
                "PDF 已生成，可点击链接下载。",
                downloadUrl(result.fileName()),
                result.fileName()
        ));
    }

    public Path resolvePdfPath(String fileName) {
        String safeFileName = PDFGenerationTool.sanitizeFileName(fileName);
        return Path.of(FileConstant.fileSaveDir(), "pdf").resolve(safeFileName).normalize();
    }

    public boolean isSafePdfFileName(String fileName) {
        return StrUtil.isNotBlank(fileName) && fileName.equals(PDFGenerationTool.sanitizeFileName(fileName));
    }

    private static String buildFileName(String chatId) {
        String safeChatId = PDFGenerationTool.sanitizeFileName(StrUtil.blankToDefault(chatId, "chat"));
        safeChatId = safeChatId.substring(0, safeChatId.length() - ".pdf".length());
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
        return safeChatId + "-" + timestamp + ".pdf";
    }

    private static String downloadUrl(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "/api/ai/files/pdf/" + encoded;
    }

    private static String compact(String prompt) {
        return StrUtil.blankToDefault(prompt, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private static String stripFrontendPromptPrefix(String prompt) {
        String text = StrUtil.blankToDefault(prompt, "").trim();
        int userTaskIndex = text.toLowerCase(Locale.ROOT).lastIndexOf("user task:");
        if (userTaskIndex >= 0) {
            return text.substring(userTaskIndex + "user task:".length()).trim();
        }
        return text;
    }
}
