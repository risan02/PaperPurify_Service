package com.sunnyday.lychat;

import org.springframework.web.multipart.MultipartFile;

public class AiFileUtils {

    // 文件内容读取方法
    public static String readFileContent(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = "";

            if (fileName != null && fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
            }

            switch (fileExtension) {
                case ".txt":
                    // 处理文本文件，自动检测编码
                    return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);

                case ".pdf":
                    // 处理PDF文件
                    return extractTextFromPDF(file);

                case ".doc":
                    // 处理旧版Word文档（.doc）
                    return extractTextFromDOC(file);

                case ".docx":
                    // 处理新版Word文档（.docx）
                    return extractTextFromDOCX(file);

                default:
                    return "不支持的文件类型: " + fileExtension + "。支持的文件类型：txt、pdf、doc、docx";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "文件读取失败: " + e.getMessage();
        }
    }

    // PDF文件解析方法
    private static String extractTextFromPDF(MultipartFile file) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper pdfStripper = new org.apache.pdfbox.text.PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    // DOC文件解析方法
    private static String extractTextFromDOC(MultipartFile file) throws Exception {
        try (org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(file.getInputStream())) {
            return doc.getDocumentText();
        }
    }

    // DOCX文件解析方法
    private static String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(file.getInputStream())) {
            org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }
}
